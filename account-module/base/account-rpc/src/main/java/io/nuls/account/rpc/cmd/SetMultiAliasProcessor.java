package io.nuls.account.rpc.cmd;

import io.nuls.kernel.model.CommandResult;
import io.nuls.kernel.processor.CommandProcessor;
import io.nuls.kernel.utils.CommandBuilder;

/**
 * @author: tag
 */
public class SetMultiAliasProcessor implements CommandProcessor {
    @Override
    public String getCommand() {
        return "setMultiAlias";
    }

    @Override
    public String getHelp() {
        CommandBuilder builder = new CommandBuilder();
        builder.newLine(getCommandDescription());
        return builder.toString();
    }

    @Override
    public String getCommandDescription() {
        return null;
    }

    @Override
    public boolean argsValidate(String[] args) {
        return false;
    }

    @Override
    public CommandResult execute(String[] args) {
        return null;
    }
}
