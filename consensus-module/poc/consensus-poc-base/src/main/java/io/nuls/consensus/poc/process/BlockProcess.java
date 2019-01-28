/*
 * *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *
 */

package io.nuls.consensus.poc.process;

import io.nuls.consensus.constant.ConsensusConstant;
import io.nuls.consensus.poc.block.validator.BifurcationUtil;
import io.nuls.consensus.poc.cache.TxMemoryPool;
import io.nuls.consensus.poc.constant.BlockContainerStatus;
import io.nuls.consensus.poc.constant.PocConsensusConstant;
import io.nuls.consensus.poc.container.BlockContainer;
import io.nuls.consensus.poc.container.ChainContainer;
import io.nuls.consensus.poc.context.ConsensusStatusContext;
import io.nuls.consensus.poc.context.PocConsensusContext;
import io.nuls.consensus.poc.manager.ChainManager;
import io.nuls.consensus.poc.model.BlockExtendsData;
import io.nuls.consensus.poc.model.Chain;
import io.nuls.consensus.poc.model.MeetingMember;
import io.nuls.consensus.poc.model.MeetingRound;
import io.nuls.consensus.poc.protocol.constant.PunishReasonEnum;
import io.nuls.consensus.poc.protocol.entity.Agent;
import io.nuls.consensus.poc.protocol.entity.RedPunishData;
import io.nuls.consensus.poc.protocol.tx.RedPunishTransaction;
import io.nuls.consensus.poc.provider.OrphanBlockProvider;
import io.nuls.consensus.poc.service.impl.RandomSeedService;
import io.nuls.consensus.poc.storage.constant.ConsensusStorageConstant;
import io.nuls.consensus.poc.storage.po.RandomSeedStatusPo;
import io.nuls.consensus.poc.storage.service.RandomSeedsStorageService;
import io.nuls.consensus.poc.storage.service.TransactionCacheStorageService;
import io.nuls.consensus.poc.util.ConsensusTool;
import io.nuls.consensus.poc.util.RandomSeedUtils;
import io.nuls.contract.constant.ContractConstant;
import io.nuls.contract.dto.ContractResult;
import io.nuls.contract.service.ContractService;
import io.nuls.contract.util.ContractUtil;
import io.nuls.core.tools.array.ArraysTool;
import io.nuls.core.tools.crypto.Hex;
import io.nuls.core.tools.json.JSONUtils;
import io.nuls.core.tools.log.BlockLog;
import io.nuls.core.tools.log.ChainLog;
import io.nuls.core.tools.log.Log;
import io.nuls.kernel.constant.TransactionErrorCode;
import io.nuls.kernel.context.NulsContext;
import io.nuls.kernel.func.TimeService;
import io.nuls.kernel.model.*;
import io.nuls.kernel.thread.manager.NulsThreadFactory;
import io.nuls.kernel.thread.manager.TaskManager;
import io.nuls.kernel.utils.AddressTool;
import io.nuls.kernel.validate.ValidateResult;
import io.nuls.ledger.service.LedgerService;
import io.nuls.protocol.base.version.NulsVersionManager;
import io.nuls.protocol.cache.TemporaryCacheManager;
import io.nuls.protocol.model.SmallBlock;
import io.nuls.protocol.service.BlockService;
import io.nuls.protocol.service.TransactionService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * @author ln
 */
public class BlockProcess {

    private BlockService blockService = NulsContext.getServiceBean(BlockService.class);

    private ChainManager chainManager;
    private OrphanBlockProvider orphanBlockProvider;
    private BifurcationUtil bifurcationUtil = BifurcationUtil.getInstance();

    private LedgerService ledgerService = NulsContext.getServiceBean(LedgerService.class);
    private TransactionService tansactionService = NulsContext.getServiceBean(TransactionService.class);
    private ContractService contractService = NulsContext.getServiceBean(ContractService.class);
    private TransactionCacheStorageService transactionCacheStorageService = NulsContext.getServiceBean(TransactionCacheStorageService.class);

    private ExecutorService signExecutor = TaskManager.createThreadPool(Runtime.getRuntime().availableProcessors(), Integer.MAX_VALUE, new NulsThreadFactory(ConsensusConstant.MODULE_ID_CONSENSUS, ""));

    private NulsProtocolProcess nulsProtocolProcess = NulsProtocolProcess.getInstance();
    private TemporaryCacheManager cacheManager = TemporaryCacheManager.getInstance();

    private RandomSeedService randomSeedService = NulsContext.getServiceBean(RandomSeedService.class);

    public BlockProcess(ChainManager chainManager, OrphanBlockProvider orphanBlockProvider) {
        this.chainManager = chainManager;
        this.orphanBlockProvider = orphanBlockProvider;
    }

    /**
     * Dealing with new blocks, the new block has two cases, the block when downloading and the latest block received, there are two different authentication logic
     *      * The download block is added. The verification round is not the current round. You need to restore the block to generate the current round status.
     *      * The new block received during operation must be verified with the latest round status
     *      * New block processing flow:
     *      * 1. Preprocessing, basic verification, including verification of block header field information, block size verification, signature verification
     *      * 2, try to add blocks to the main chain, first verify the block of the round and packaged people, if the verification fails, then put into the isolated block pool, if the verification is successful, then add into the main chain, add the memory state into
     *      * 3, verify the transaction of the block is legitimate, whether there are double flowers or other illegal transactions, if there is, then put in the isolated block pool, if not, save the block
     *      * 4, save the block header information, save the block transaction information
     *      * 5. Forwarding block
     * <p>
     * 处理新的区块，新区块有两种情况，下载时的区块和接收到的最新区块，两种有不同的验证逻辑
     * 下载中区块的添加，验证的轮次不是当前的轮次，需要还原区块产生当时的轮次状态
     * 运行中接收到的新区块，必须以当前最新的轮次状态来验证
     * 新区块处理的流程：
     * 1、预处理，做基本的验证，包括区块头字段信息的验证，区块大小的验证，签名的验证
     * 2、尝试向主链添加区块，先验证区块的轮次和打包人，如果验证失败则放入孤立区块池，如果验证成功，则添加进主链里，内存状态添加进去
     * 3、验证区块的交易是否合法，是否有双花或者其它不合法的交易，如果有，则放入孤立区块池里，如果没有，则保存区块
     * 4、保存区块头信息，保存区块交易信息
     * 5、转发区块
     *
     * @return boolean
     */
    public boolean addBlock(BlockContainer blockContainer) throws IOException {

        if (NulsContext.mastUpGrade == true) {
            return false;
        }

        boolean isDownload = blockContainer.getStatus() == BlockContainerStatus.DOWNLOADING;
        Block block = blockContainer.getBlock();
//        Log.info("******** BlockProcess - addBlock ******* height:{} , hash:{}, preHash:{}", block.getHeader().getHeight(), block.getHeader().getHash(), block.getHeader().getPreHash());
//        Log.info(" packingAddress:{}", AddressTool.getStringAddressByBytes(block.getHeader().getPackingAddress()));
        // Discard future blocks
        // 丢弃掉未来时间的区块
        if (TimeService.currentTimeMillis() + PocConsensusConstant.DISCARD_FUTURE_BLOCKS_TIME < block.getHeader().getTime()) {
            return false;
        }
        // Verify the the block, the content to be verified includes: whether the block size exceeds the limit,
        // whether the attribute of the block header is legal, the Merkel tree root is correct, the signature is correct,
        // and whether the expanded round of information is valid
        // 验证区块，需要验证的内容有：区块大小是否超过限制、区块头属性是否合法、梅克尔树根是否正确、签名是否正确、扩展的轮次信息是否合法
        block.verifyWithException();
        bifurcationUtil.validate(block.getHeader());

        ValidateResult<List<Transaction>> validateResult = ledgerService.verifyDoubleSpend(block);
        if (validateResult.isFailed() && validateResult.getErrorCode().equals(TransactionErrorCode.TRANSACTION_REPEATED)) {
            RedPunishTransaction redPunishTransaction = new RedPunishTransaction();
            RedPunishData redPunishData = new RedPunishData();
            byte[] packingAddress = AddressTool.getAddress(block.getHeader().getBlockSignature().getPublicKey());
            List<Agent> agentList = PocConsensusContext.getChainManager().getMasterChain().getChain().getAgentList();
            Agent agent = null;
            for (Agent a : agentList) {
                if (a.getDelHeight() > 0) {
                    continue;
                }
                if (Arrays.equals(a.getPackingAddress(), packingAddress)) {
                    agent = a;
                    break;
                }
            }
            if (null == agent) {
                return false;
            }
            redPunishData.setAddress(agent.getAgentAddress());
            SmallBlock smallBlock = new SmallBlock();
            smallBlock.setHeader(block.getHeader());
            smallBlock.setTxHashList(block.getTxHashList());
            for (Transaction tx : validateResult.getData()) {
                smallBlock.addBaseTx(tx);
            }
            redPunishData.setEvidence(smallBlock.serialize());
            redPunishData.setReasonCode(PunishReasonEnum.DOUBLE_SPEND.getCode());
            redPunishTransaction.setTxData(redPunishData);
            redPunishTransaction.setTime(smallBlock.getHeader().getTime());
            CoinData coinData = ConsensusTool.getStopAgentCoinData(agent, redPunishTransaction.getTime() + PocConsensusConstant.RED_PUNISH_LOCK_TIME);
            redPunishTransaction.setCoinData(coinData);
            redPunishTransaction.setHash(NulsDigestData.calcDigestData(redPunishTransaction.serializeForHash()));
            TxMemoryPool.getInstance().add(redPunishTransaction, false);
            return false;
        }

        // Verify that the block round information is correct, if correct, join the main chain
        // 验证区块轮次信息是否正确、如果正确，则加入主链
        Result verifyAndAddBlockResult = chainManager.getMasterChain().verifyAndAddBlock(block, isDownload, false);
        if (verifyAndAddBlockResult.isSuccess()) {
            boolean success = true;
            try {
                do {
                    // Verify that the block transaction is valid, save the block if the verification passes, and discard the block if it fails
                    // 验证区块交易是否合法，如果验证通过则保存区块，如果失败则丢弃该块
                    long time = System.currentTimeMillis();
                    List<Future<Boolean>> futures = new ArrayList<>();

                    List<Transaction> txs = block.getTxs();

                    //首先验证区块里的所有交易是否都属于当前版本的交易，如果有不包含的交易类型，丢弃该区块
                    Set<Integer> txTypeSet = NulsVersionManager.getMainProtocolContainer().getTxMap().keySet();
                    for (Transaction tx : txs) {
                        if (!txTypeSet.contains(tx.getType())) {
                            Log.info("--------------------- block tx discard, current protocol version:" + NulsVersionManager.getMainProtocolContainer().getVersion() + ",  tx.type:" + tx.getType());
                            return false;
                        }
                    }

                    for (Transaction tx : txs) {
                        Future<Boolean> res = signExecutor.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                ValidateResult verify = tx.verify();
                                /** ************************************************************/
                                if (verify.isFailed()) {
                                    Log.error(JSONUtils.obj2json(verify.getErrorCode()));
                                }
                                /** ************************************************************/
                                boolean result = verify.isSuccess();
                                return result;
                            }
                        });
                        futures.add(res);
                    }

                    Map<String, Coin> toMaps = new HashMap<>();
                    Set<String> fromSet = new HashSet<>();

                    /**
                     * pierre add 智能合约相关
                     */
                    BlockHeader bestBlockHeader = NulsContext.getInstance().getBestBlock().getHeader();
                    BlockHeader verifyHeader = block.getHeader();
                    long bestHeight = bestBlockHeader.getHeight();
                    byte[] receiveStateRoot = ConsensusTool.getStateRoot(verifyHeader);
                    byte[] stateRoot = ConsensusTool.getStateRoot(bestBlockHeader);
                    ContractResult contractResult;
                    Map<String, Coin> contractUsedCoinMap = new HashMap<>();
                    int totalGasUsed = 0;

                    // 为本次验证区块增加一个合约的临时余额区，用于记录本次合约地址余额的变化
                    contractService.createContractTempBalance();
                    // 为本次验证区块创建一个批量执行合约的执行器
                    contractService.createBatchExecute(stateRoot);
                    // 为本次验证区块存储当前块的部分区块头信息(高度、时间、打包者地址)
                    BlockHeader tempHeader = new BlockHeader();
                    tempHeader.setTime(verifyHeader.getTime());
                    tempHeader.setHeight(verifyHeader.getHeight());
                    tempHeader.setPackingAddress(verifyHeader.getPackingAddress());
                    contractService.createCurrentBlockHeader(tempHeader);

                    List<ContractResult> contractResultList = new ArrayList<>();
                    // 用于存储合约执行结果的stateRoot, 如果不为空，则说明验证、打包的区块是同一个节点
                    byte[] tempStateRoot = null;

                    for (Transaction tx : txs) {

                        if (tx.isSystemTx()) {
                            continue;
                        }

                        // 区块中可以消耗的最大Gas总量，超过这个值，如果还有消耗GAS的合约交易，则本区块中不再继续验证区块
                        if (totalGasUsed > ContractConstant.MAX_PACKAGE_GAS) {
                            if (ContractUtil.isGasCostContractTransaction(tx)) {
                                Log.info("verify block failed: Excess contract transaction detected.");
                                success = false;
                                break;
                            }
                        }

                        ValidateResult result = ledgerService.verifyCoinData(tx, toMaps, fromSet);
                        if (result.isFailed()) {
                            Log.info("failed message:" + result.getMsg());
                            success = false;
                            break;
                        }

                        // 验证时发现智能合约交易就调用智能合约
                        if (ContractUtil.isContractTransaction(tx)) {
                            contractResult = contractService.batchProcessTx(tx, bestHeight, block, stateRoot, toMaps, contractUsedCoinMap, false).getData();
                            if (contractResult != null) {
                                tempStateRoot = contractResult.getStateRoot();
                                totalGasUsed += contractResult.getGasUsed();
                                contractResultList.add(contractResult);
                            }
                        }

                    }

                    if (!success) {
                        break;
                    }

                    // 验证结束后移除临时余额区
                    contractService.removeContractTempBalance();
                    stateRoot = contractService.commitBatchExecute().getData();
                    // 验证结束后移除批量执行合约的执行器
                    contractService.removeBatchExecute();
                    // 验证结束后移除当前区块头信息
                    contractService.removeCurrentBlockHeader();
                    // 如果不为空，则说明验证、打包的区块是同一个节点
                    if (tempStateRoot != null) {
                        stateRoot = tempStateRoot;
                    }
                    block.getHeader().setStateRoot(stateRoot);
                    for (ContractResult result : contractResultList) {
                        result.setStateRoot(stateRoot);
                    }

                    // 验证世界状态根
                    if ((receiveStateRoot != null || stateRoot != null) && !Arrays.equals(receiveStateRoot, stateRoot)) {
                        Log.info("contract stateRoot incorrect. receiveStateRoot is {}, stateRoot is {}.", receiveStateRoot != null ? Hex.encode(receiveStateRoot) : receiveStateRoot, stateRoot != null ? Hex.encode(stateRoot) : stateRoot);
                        success = false;
                        break;
                    }

                    // 验证CoinBase交易
                    Object[] objects = (Object[]) verifyAndAddBlockResult.getData();
                    MeetingRound currentRound = (MeetingRound) objects[0];
                    MeetingMember member = (MeetingMember) objects[1];
                    if (!chainManager.getMasterChain().verifyCoinBaseTx(block, currentRound, member)) {
                        success = false;
                        break;
                    }

                    if (!success) {
                        break;
                    }

                    ValidateResult validateResult1 = tansactionService.conflictDetect(block.getTxs());
                    if (validateResult1.isFailed()) {
                        success = false;
                        Log.info("failed message:" + validateResult1.getMsg());
                        break;
                    }

                    for (Future<Boolean> future : futures) {
                        if (!future.get()) {
                            success = false;
                            Log.info("verify failed!");
                            break;
                        }
                    }
//                    Log.info("验证交易耗时：" + (System.currentTimeMillis() - time));
                    if (!success) {
                        break;
                    }
                    time = System.currentTimeMillis();

                    // save block
                    Result result = blockService.saveBlock(block);
                    success = result.isSuccess();
                    if (!success) {
                        Log.warn("save block fail : reason : " + result.getMsg() + ", block height : " + block.getHeader().getHeight() + ", hash : " + block.getHeader().getHash());
                    } else {
                        if (NulsVersionManager.getMainVersion() >= 3) {
                            randomSeedService.processBlock(block.getHeader(), bestBlockHeader);
                        }
                        RewardStatisticsProcess.addBlock(block);
                        //更新版本协议内容
                        nulsProtocolProcess.processProtocolUpGrade(block.getHeader());
                        BlockLog.debug("save block height : " + block.getHeader().getHeight() + " , hash : " + block.getHeader().getHash());
                    }
//                    Log.info("保存耗时：" + (System.currentTimeMillis() - time));
                } while (false);
            } catch (Exception e) {
                Log.error("save block error : " + e.getMessage(), e);
            }
            if (success) {
                long t = System.currentTimeMillis();
                NulsContext.getInstance().setBestBlock(block);
                // remove tx from memory pool
                removeTxFromMemoryPool(block);
//                Log.info("移除内存交易耗时：" + (System.currentTimeMillis() - t));
                // 转发区块
                forwardingBlock(blockContainer);
//                Log.info("转发区块耗时：" + (System.currentTimeMillis() - t));

                return true;
            } else {
                contractService.removeContractTempBalance();
                contractService.removeBatchExecute();
                contractService.removeCurrentBlockHeader();

                chainManager.getMasterChain().rollback(block);
                NulsContext.getInstance().setBestBlock(chainManager.getBestBlock());

                Log.error("save block fail : " + block.getHeader().getHeight() + " , isDownload : " + isDownload);
            }
        } else {
            // Failed to block directly in the download
            // 下载中验证失败的区块直接丢弃
            if (isDownload && !ConsensusStatusContext.isRunning()) {
                return false;
            }
            boolean hasFoundForkChain = checkAndAddForkChain(block);
            if (!hasFoundForkChain) {

                ChainLog.debug("add block {} - {} in queue", block.getHeader().getHeight(), block.getHeader().getHash().getDigestHex());

                orphanBlockProvider.addBlock(blockContainer);
            }
        }
        return false;
    }

    /**
     * forwarding block
     * <p>
     * 转发区块
     */
    private void forwardingBlock(BlockContainer blockContainer) {
        if (blockContainer.getStatus() == BlockContainerStatus.DOWNLOADING) {
            return;
        }
        if (blockContainer.getNode() == null) {
            return;
        }
        SmallBlock smallBlock = ConsensusTool.getSmallBlock(blockContainer.getBlock());
        cacheManager.cacheSmallBlock(smallBlock);
        Result result = blockService.forwardBlock(blockContainer.getBlock().getHeader().getHash(), blockContainer.getNode());
        if (!result.isSuccess()) {
            Log.warn("forward the block failed, block height: " + blockContainer.getBlock().getHeader().getHeight() + " , hash : " + blockContainer.getBlock().getHeader().getHash());
        }
    }

    /**
     * The transaction is confirmed and the transaction in the memory pool is removed
     * <p>
     * 交易被确认，移除内存池里面存在的交易
     *
     * @return boolean
     */
    public boolean removeTxFromMemoryPool(Block block) {
        boolean success = true;
        for (Transaction tx : block.getTxs()) {
            transactionCacheStorageService.removeTx(tx.getHash());
        }
        return success;
    }

    /**
     * When a new block cannot be added to the main chain, it may exist on a forked chain, or it may be that the local main chain is not the latest one.
     * When this happens, it is necessary to check whether the block is forked with the main chain or connected with an already existing forked chain.
     * if you can combine into a new branch chain, add a new branch chain
     * <p>
     * 当一个新的区块，不能被添加进主链时，那么它有可能存在于一条分叉链上，也有可能是本地主链不是最新的网络主链
     * 出现这种情况时，需要检测该区块是否与主链分叉或者与已经存在的分叉链相连，如果能组合成一条新的分叉链，则添加新的分叉链
     *
     * @return boolean
     */
    protected boolean checkAndAddForkChain(Block block) {
        // check the preHash is in the other chain
        boolean hasFoundForkChain = checkForkChainFromForkChains(block);
        if (hasFoundForkChain) {
            return hasFoundForkChain;
        }
        return checkForkChainFromMasterChain(block);
    }

    /**
     * When a block cannot be connected to the main chain, it is checked whether it is a branch of the main chain.
     * If it is a branch of the main chain, a bifurcation chain is generated, and then the bifurcation chain is added into the forked chain pool to be verified.
     * <p>
     * 当一个区块不能与主链相连时，检查是否是主链的分支，如果是主链的分支，则产生一条分叉链，然后把该分叉链添加进待验证的分叉链池里
     *
     * @return boolean
     */
    protected boolean checkForkChainFromMasterChain(Block block) {

        BlockHeader blockHeader = block.getHeader();

        Chain masterChain = chainManager.getMasterChain().getChain();
        List<BlockHeader> headerList = masterChain.getAllBlockHeaderList();

        for (int i = headerList.size() - 1; i >= 0; i--) {
            BlockHeader header = headerList.get(i);

            if (header.getHash().equals(blockHeader.getHash())) {
                // found a same block , return true
                return true;
            } else if (header.getHash().equals(blockHeader.getPreHash())) {

                if (header.getHeight() + 1L != blockHeader.getHeight()) {
                    // Discard data blocks that are incorrect
                    // 丢弃数据不正确的区块
                    return true;
                }
                Chain newForkChain = new Chain();

                newForkChain.addBlock(block);

                chainManager.getChains().add(new ChainContainer(newForkChain));
                return true;
            }

            if (header.getHeight() < blockHeader.getHeight()) {
                break;
            }
        }
        return false;
    }

    /**
     * When a block cannot be connected to the main chain, it checks whether it is a branch of a forked chain,
     * or is connected to a forked chain, and if so, it produces a forked chain, and then adds the forked chain into the fork to be verified. Chain pool;
     * or add the block directly to the corresponding branch chain
     * <p>
     * 当一个区块不能与主链相连时，检查是否是分叉链的分支，或者与分叉链相连，如果是，则产生一条分叉链，然后把该分叉链添加进待验证的分叉链池里；或者把该块直接添加到对应的分叉链上
     *
     * @return boolean
     */
    protected boolean checkForkChainFromForkChains(Block block) {

        BlockHeader blockHeader = block.getHeader();
        NulsDigestData preHash = blockHeader.getPreHash();

        // check the preHash is in the waitVerifyChainList

        Iterator<ChainContainer> iterator = chainManager.getChains().iterator();
        while (iterator.hasNext()) {
            Chain forkChain = iterator.next().getChain();
            List<BlockHeader> headerList = forkChain.getAllBlockHeaderList();
            int size = headerList.size();
            for (int i = size - 1; i >= 0; i--) {
                BlockHeader header = headerList.get(i);

                if (header.getHash().equals(blockHeader.getHash())) {
                    // found a same block , return true
                    return true;
                } else if (header.getHash().equals(preHash)) {

                    if (header.getHeight() + 1L != blockHeader.getHeight()) {
                        // Discard data blocks that are incorrect
                        // 丢弃数据不正确的区块
                        return true;
                    }

                    // Check whether it is forked or connected. If it is a connection, add it.
                    // 检查是分叉还是连接，如果是连接，则加上即可
                    if (i == size - 1) {
                        forkChain.addBlock(block);
                        return true;
                    }

                    // The block is again forked in the forked chain
                    // 该块是在分叉链中再次进行的分叉
                    List<Block> blockList = forkChain.getAllBlockList();

                    Chain newForkChain = new Chain();

                    newForkChain.initData(forkChain.getStartBlockHeader(), new ArrayList<>(headerList.subList(0, i + 1)), new ArrayList<>(blockList.subList(0, i + 1)));
                    newForkChain.addBlock(block);


                    return chainManager.getChains().add(new ChainContainer(newForkChain));
                } else if (header.getHeight() < blockHeader.getHeight()) {
                    break;
                }
            }
        }
        return false;
    }

}
