webpackJsonp([26],{DgPo:function(t,a,e){"use strict";Object.defineProperty(a,"__esModule",{value:!0});var s=e("3cXf"),n=e.n(s),o=e("HzJ8"),i=e.n(o),r=e("LPk9"),c=e("6ROu"),l=e.n(c),v=e("x47x"),u=e("YgNb"),_=(e("2tLR"),e("QmSG")),d={data:function(){return{hash:this.$route.query.hash,type:this.$route.query.type,infoData:[],inputs:[],allInputs:0,outputs:[],allOutputs:0,times:"",contractIf:!1,callIf:!1,insideIf:!1,tokenIf:!1,runing:_.c}},components:{Back:r.a},created:function(){this.getHashInfo(this.hash,this.type.toString())},methods:{getHashInfo:function(t,a){var e=this,s="";document.getElementById("outPre");s="100"===a||"101"===a||"102"===a||"103"===a||"1000"===a?"/contract/tx/"+t:"/accountledger/tx/"+t,this.$fetch(s).then(function(t){if(e.infoData=t.data,e.infoData.fee=Object(u.b)(t.data.fee).toString(),e.times=l()(Object(u.i)(t.data.time)).format("YYYY-MM-DD HH:mm:ss"),t.data.remark=t.data.remark?Object(u.j)(t.data.remark):"",t.data.inputs.length>0){var a=!0,s=!1,o=void 0;try{for(var r,c=i()(t.data.inputs);!(a=(r=c.next()).done);a=!0){var _=r.value;_.value=Object(u.m)(_.value,8),e.allInputs=Object(v.BigNumber)(e.allInputs).plus(_.value).toString()}}catch(t){s=!0,o=t}finally{try{!a&&c.return&&c.return()}finally{if(s)throw o}}}if(e.inputs=t.data.inputs,t.data.outputs.length>0){var d=!0,f=!1,m=void 0;try{for(var p,h=i()(t.data.outputs);!(d=(p=h.next()).done);d=!0){var g=p.value;g.value=Object(u.m)(g.value,8),e.allOutputs=Object(v.BigNumber)(e.allOutputs).plus(g.value).toString()}}catch(t){f=!0,m=t}finally{try{!d&&h.return&&h.return()}finally{if(f)throw m}}}if(e.outputs=t.data.outputs,100===t.data.type||101===t.data.type||102===t.data.type||1e3===t.data.type){if(e.contractIf=!0,100===t.data.type);else if(101===t.data.type){if(e.callIf=!0,t.data.callName=t.data.txData.data.methodName,t.data.txData.data.args.length>0&&(document.getElementById("out_pre").innerText=n()(t.data.txData.data.args,null,0)),t.data.callResult=t.data.contractResult.result,t.data.contractResult.transfers.length>0){e.insideIf=!0,t.data.insideUnit="NULS";var I=t.data.contractResult.transfers;for(var w in I){"0"===w.toString()?I[w].name=e.$t("message.c222"):I[w].name="";var b=new v.BigNumber(Object(u.b)(I[w].value).toString());I[w].value=b.toFormat().replace(/[,]/g,"")}t.data.insideItme=t.data.contractResult.transfers}if(t.data.contractResult.tokenTransfers.length>0){e.tokenIf=!0;var D=t.data.contractResult.tokenTransfers;for(var x in D){"0"===x.toString()?D[x].name="Token Transfer":D[x].name="";var y=Object(u.d)(D[x].decimals),k=new v.BigNumber(Object(u.a)(D[x].value,y).toString());D[x].value=k.toFormat().replace(/[,]/g,"")}t.data.tokenItme=t.data.contractResult.tokenTransfers}}else e.contractIf=!1,t.data.txDataHexString="",t.data.contractAddress=t.data.contractResult.contractAddress;t.data.totalFee=Object(u.b)(t.data.contractResult.totalFee).toString(),t.data.txSizeFee=Object(u.b)(t.data.contractResult.txSizeFee).toString(),t.data.actualContractFee=Object(u.b)(t.data.contractResult.actualContractFee).toString(),t.data.refundFee=Object(u.b)(t.data.contractResult.refundFee).toString(),t.data.contractAddress=t.data.contractResult.contractAddress,t.data.gasLimit=t.data.contractResult.gasLimit,t.data.price=t.data.contractResult.price,t.data.gasUsed=t.data.contractResult.gasUsed,t.data.modelIf=t.data.contractResult.success,t.data.errorMessage=t.data.contractResult.errorMessage;var S=t.data.txData.data;document.getElementById("outPre").innerText=n()(S,null,2)}})},hashCopy:function(t){this.runing?t.length>50?window.open("https://nulscan.io/transactionHash?hash="+t,"_blank"):window.open("https://nulscan.io/accountInfo?address="+t,"_blank"):t.length>50?window.open("http://testnet.nulscan.io/transactionHash?hash="+t,"_blank"):window.open("http://testnet.nulscan.io/accountInfo?address="+t,"_blank")}},beforeRouteLeave:function(t,a,e){"deallist"!==t.name&&(sessionStorage.removeItem("dealListTotalAll"),sessionStorage.removeItem("dealListAssets"),sessionStorage.removeItem("nulsIf"),sessionStorage.removeItem("dealListTypes"),sessionStorage.removeItem("dealListPages")),e()}},f={render:function(){var t=this,a=t.$createElement,e=t._self._c||a;return e("div",{staticClass:"deal-info"},[e("Back",{attrs:{backTitle:this.$t("message.transactionManagement")}}),t._v(" "),e("div",{staticClass:"deal-info-top"},[e("div",{staticClass:"deal-left fl"},[e("div",[t._v(t._s(t.$t("message.input"))),e("span",[t._v(" "+t._s(this.allInputs.toString())+" NULS")])]),t._v(" "),e("ul",t._l(t.inputs,function(a){return e("li",[e("label",{staticClass:"cursor-p",on:{click:function(e){t.hashCopy(a.address)}}},[t._v(t._s(a.address))]),t._v(" "),e("span",[t._v(t._s(a.value.toString()))])])}))]),t._v(" "),e("div",{staticClass:"deal-right fr"},[e("div",[t._v(t._s(t.$t("message.output"))),e("span",[t._v(t._s(this.allOutputs.toString())+" NULS")])]),t._v(" "),e("ul",t._l(t.outputs,function(a){return e("li",[e("label",{staticClass:"cursor-p",on:{click:function(e){t.hashCopy(a.address)}}},[t._v(t._s(a.address))]),t._v(" "),e("span",[t._v(t._s(a.value.toString()))])])}))])]),t._v(" "),e("div",{staticClass:"deal-case"},[e("h3",[t._v(t._s(t.$t("message.overview")))]),t._v(" "),e("ul",[e("li",[e("span",[t._v(t._s(t.$t("message.tradingTime")))]),t._v(t._s(this.times))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:!t.contractIf,expression:"!contractIf"}]},[e("span",[t._v(t._s(t.$t("message.miningFee1")))]),t._v(t._s(t.infoData.fee)+" NULS")]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf,expression:"contractIf"}]},[e("span",[t._v(t._s(t.$t("message.miningFee1")))]),t._v("\n        "+t._s(this.infoData.totalFee)+t._s(t.$t("message.c210"))+"=\n        "+t._s(this.infoData.refundFee)+t._s(t.$t("message.c213"))+"\n        "),e("el-tooltip",{staticClass:"item",attrs:{effect:"dark",content:t.$t("message.c251"),placement:"bottom"}},[e("i",{staticClass:"el-icon-question"})]),t._v(" +\n        "+t._s(this.infoData.actualContractFee)+t._s(t.$t("message.types"+t.infoData.type))+"+\n        "+t._s(this.infoData.txSizeFee)+t._s(t.$t("message.c211"))+"\n\n        "),e("label",{staticClass:"unit"},[t._v(t._s(t.$t("message.c214"))+": NULS")])],1),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.autograph")))]),e("label",{staticClass:"text-ds cursor-p",on:{click:function(a){t.hashCopy(t.infoData.hash)}}},[t._v(t._s(t.infoData.hash))])]),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.transactionType")))]),t._v(t._s(t.$t("message.type"+t.infoData.type)))]),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.transactionState")))]),t._v(t._s(t.$t("message.statusS"+t.infoData.status)))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf||102===t.infoData.type,expression:"contractIf || infoData.type === 102 "}]},[e("span",[t._v(t._s(t.$t("message.c215")))]),t._v(t._s(t.infoData.contractAddress))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf||102===t.infoData.type,expression:"contractIf || infoData.type === 102 "}]},[e("span",[t._v(t._s(t.$t("message.c247")))]),t._v(" "),e("font",{class:t.infoData.modelIf?"success":"failed"},[t._v(t._s(t.$t("message.c"+t.infoData.modelIf)))]),t._v(" "),e("font",{directives:[{name:"show",rawName:"v-show",value:!t.infoData.modelIf,expression:"!infoData.modelIf"}],class:t.infoData.modelIf?"success":"failed"},[t._v("( "+t._s(t.infoData.errorMessage)+" )")])],1),t._v(" "),t._l(t.infoData.insideItme,function(a){return e("li",{directives:[{name:"show",rawName:"v-show",value:t.insideIf,expression:"insideIf"}]},[e("span",[t._v(t._s(""===a.name?" ":a.name))]),t._v("\n        From "),e("label",{staticClass:"text-ds cursor-p",on:{click:function(e){t.hashCopy(a.from)}}},[t._v(t._s(a.from))]),t._v("\n        To "),e("label",{staticClass:"text-ds cursor-p",on:{click:function(e){t.hashCopy(a.to)}}},[t._v(t._s(a.to))]),t._v("\n        for "),e("label",{staticClass:"text-ds"},[t._v(t._s(a.value))]),t._v(" "+t._s(t.infoData.insideUnit)+"\n        "),e("p",[e("span",[t._v(" ")]),t._v("TXID: "),e("label",{staticClass:"text-ds cursor-p",on:{click:function(e){t.hashCopy(a.to)}}},[t._v(t._s(a.txHash))])])])}),t._v(" "),t._l(t.infoData.tokenItme,function(a){return e("li",{directives:[{name:"show",rawName:"v-show",value:t.tokenIf,expression:"tokenIf"}]},[e("span",[t._v(t._s(a.name)+" ")]),t._v("\n        From "),e("label",{staticClass:"text-ds cursor-p",on:{click:function(e){t.hashCopy(a.from)}}},[t._v(t._s(a.from))]),t._v("\n        To "),e("label",{staticClass:"text-ds cursor-p",on:{click:function(e){t.hashCopy(a.to)}}},[t._v(t._s(a.to))]),t._v("\n        for "),e("label",{staticClass:"text-ds"},[t._v(t._s(a.value))]),t._v(" "+t._s(a.symbol)+"\n        "),e("p",[e("span",[t._v(" ")]),t._v(t._s(t.$t("message.c215"))+": "),e("label",{},[t._v(t._s(a.contractAddress))])])])}),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf,expression:"contractIf"}]},[e("span",[t._v("GasLimit")]),t._v(t._s(t.infoData.gasLimit))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf,expression:"contractIf"}]},[e("span",[t._v(t._s(t.$t("message.c216")))]),t._v(t._s(t.infoData.price))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.contractIf,expression:"contractIf"}]},[e("span",[t._v("GasUsed")]),t._v(t._s(t.infoData.gasUsed))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.callIf,expression:"callIf"}]},[e("span",[t._v(t._s(t.$t("message.c217")))]),t._v(t._s(t.$t("message.c218"))+": "+t._s(t.infoData.callName))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.callIf,expression:"callIf"}]},[e("span",[t._v(" ")]),t._v(t._s(t.$t("message.c219"))+":\n        "),e("label",{attrs:{id:"out_pre"}})]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:t.callIf,expression:"callIf"}]},[e("span",[t._v(" ")]),t._v(t._s(t.$t("message.c2201"))+": "+t._s(t.infoData.callResult))]),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.blockHeight")))]),t._v(t._s(t.infoData.blockHeight<0?"- -":t.infoData.blockHeight))]),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.c221")))]),t._v(t._s(t.infoData.confirmCount))]),t._v(" "),e("li",{directives:[{name:"show",rawName:"v-show",value:!1,expression:"false"}]},[e("span",[t._v("TxData")]),t._v(" "),e("pre",{directives:[{name:"show",rawName:"v-show",value:!t.contractIf,expression:"!contractIf"}],staticClass:"out-pre"},[t._v(t._s(""===t.infoData.txDataHexString?" ":t.infoData.txDataHexString))]),t._v(" "),e("pre",{directives:[{name:"show",rawName:"v-show",value:t.contractIf,expression:"contractIf"}],staticClass:"out-pre",attrs:{id:"outPre"}},[t._v(" ")])]),t._v(" "),e("li",[e("span",[t._v(t._s(t.$t("message.remarks")))]),t._v(t._s(""===t.infoData.remark?" ":t.infoData.remark))])],2)])],1)},staticRenderFns:[]};var m=e("vSla")(d,f,!1,function(t){e("VVEr")},null,null);a.default=m.exports},VVEr:function(t,a){}});