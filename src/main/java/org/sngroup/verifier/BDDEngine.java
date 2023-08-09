package org.sngroup.verifier;

import org.sngroup.util.IPPrefix;
import org.sngroup.util.Utility;
import jdd.bdd.BDD;
import jdd.util.Allocator;

import java.util.List;
import java.util.logging.Logger;

public class BDDEngine {
    public static void main(String[] args) {
        BDDEngine bddEngine = new BDDEngine();
        int p1 = bddEngine.encodeDstIPPrefix(167772160L, 8);
        bddEngine.printPredicate(p1);
    }
    protected static final Logger logger = Logger.getGlobal();
    public final static int BDDFalse = 0;
    public final static int BDDTrue = 1;

    final static int protocolBits = 8;
    final static int portBits = 16;
    final static int ipBits = 32;

    final static int srcIPStartIndex = 0;
    final static int dstIPStartIndex = srcIPStartIndex + ipBits;
    final static int srcPortStartIndex = dstIPStartIndex + ipBits;
    final static int dstPortStartIndex = srcPortStartIndex + portBits;
    final static int protocolStartIndex = dstPortStartIndex + portBits;

    final static int size = protocolStartIndex + protocolBits;

    private char[] set_chars = null;
    int[] protocol;
    int[] srcPort;
    int[] dstPort;
    int[] srcIP;
    int[] dstIP;
    TSBDD bdd;

    int[] vars;

    int[] dstIPField;
    public BDDEngine(){
        bdd = new TSBDD(new BDD(10000, 10000));

        protocol = new int[protocolBits];
        srcPort = new int[portBits];
        dstPort = new int[portBits];
        srcIP = new int[ipBits];
        dstIP = new int[ipBits];
        dstIPField = new int[ipBits];
        /**
         * will try more orders of variables
         */
        DeclareSrcIP();
        DeclareDstIP();
        DeclareSrcPort();
        DeclareDstPort();
        DeclareProtocol();

        dstIPField = AndInBatch(dstIP);
    }

    private void DeclareProtocol() {
        DeclareVars(protocol, protocolBits);
    }

    private void DeclareSrcPort() {
        DeclareVars(srcPort, portBits);
    }

    private void DeclareDstPort() {
        DeclareVars(dstPort, portBits);
    }

    private void DeclareSrcIP() {
        DeclareVars(srcIP, ipBits);
    }

    private void DeclareDstIP() {
        DeclareVars(dstIP, ipBits);
    }

    public TSBDD getBDD(){
        return bdd;
    }

    public void printPredicate(int p){
        printSet(p);
    }
    public String printSet(int p)  {
        if( p < 2) {
            String result = String.format("%s", (p == 0) ? "null" : "all");
            System.out.println(result);
            return result;
//            logger.info(String.format("%s\n", (p == 0) ? "null packet" : "any packet"));

        } else {
            StringBuilder sb = new StringBuilder();
//            sb.append(p).append("\n");
//            logger.info(String.valueOf(p));

            if(set_chars == null || set_chars.length < size)
                set_chars = Allocator.allocateCharArray(size);

            printSet_rec(p, 0, sb);
            System.out.println(sb.toString());
            return sb.toString();
//            System.out.print("\n");
        }
    }

    public String getSet(int p)  {
        if( p < 2) {
            String result = String.format("%s", (p == 0) ? "null" : "all");
//            System.out.println(result);
            return result;
//            logger.info(String.format("%s\n", (p == 0) ? "null packet" : "any packet"));

        } else {
            StringBuilder sb = new StringBuilder();
//            sb.append(p).append("\n");
//            logger.info(String.valueOf(p));

            if(set_chars == null || set_chars.length < size)
                set_chars = Allocator.allocateCharArray(size);

            printSet_rec(p, 0, sb);
//            System.out.println(sb.toString());
            return sb.toString();
//            System.out.print("\n");
        }
    }

    private void printSet_rec(int p, int level, StringBuilder sb) {
        if(level == size) {
//            sb.append(String.format("src IP:\"%s\", src port:\"%s\", dst IP:\"%s\", dst port:\"%s\", protocol: \"%s\"\n",
//                        parseIP(srcIPStartIndex),
//                        parseNumber(srcPortStartIndex, portBits),
//                        parseIP(dstIPStartIndex),
//                        parseNumber(dstPortStartIndex, portBits),
//                        parseNumber(protocolStartIndex, protocolBits)
//                    ));
            sb.append(String.format("%s;",
                    parseIP(dstIPStartIndex)
            ));
        // todo
            return;
        }
        BDD bdd = getBDD().bdd;
        int var = bdd.getVar(p);
        if(var > level || p == 1 ) {
            set_chars[level] = '-';
            printSet_rec(p, level+1, sb);
            return;
        }

        int low = bdd.getLow(p);
        int high = bdd.getHigh(p);

        if(low != 0) {
            set_chars[level] = '0';
            printSet_rec(low, level+1, sb);
        }

        if(high != 0) {
            set_chars[level] = '1';
            printSet_rec(high, level+1, sb);
        }
    }

    private String parseIP(int start){
        if(set_chars.length < start+31){
            System.err.println("Wrong ip!");
            return "";
        }

        int prefix= 32;
        boolean hasDash = false, hasMidDash = false, hasNumber = false;

        for(int i=prefix-1; i>=0; i--){
            hasDash |= set_chars[start+i] == '-';
            if(!hasNumber && (set_chars[start+i] == '1' || set_chars[start+i] == '0')){
                hasNumber = true;
                prefix = i+1;
            }
            hasMidDash |= set_chars[start+i] == '-' && hasNumber;
        }
        if(!hasNumber) return "any";
        if(!hasMidDash) {
            return Utility.charToInt8bit(set_chars, start) + "." +
                    Utility.charToInt8bit(set_chars, start+8) + "." +
                    Utility.charToInt8bit(set_chars, start+16) + "." +
                    Utility.charToInt8bit(set_chars, start+24) + (prefix==32?"":"/"+prefix);
        }
        return "not implement";
    }

    private String parseNumber(int start, int length){
        if(set_chars.length < start+length){
            System.err.println("Wrong number!");
            return "";
        }

        int prefix= length;
        boolean hasDash = false, hasMidDash = false, hasNumber = false;

        for(int i=prefix-1; i>=0; i--){
            hasDash |= set_chars[start+i] == '-';
            if(!hasNumber && (set_chars[start+i] == '1' || set_chars[start+i] == '0')){
                hasNumber = true;
                prefix = i+1;
            }
            hasMidDash |= set_chars[start+i] == '-' && hasNumber;
        }
        if(!hasNumber) return "any";
        if(!hasMidDash) {
            if(!hasDash) return Utility.charToInt(set_chars, start, length);
            for(int i=prefix;i<length;i++){
                set_chars[i] = '0';
            }
            String begin = Utility.charToInt(set_chars, start, length);
            for(int i=prefix;i<length;i++){
                set_chars[i] = '1';
            }
            String end = Utility.charToInt(set_chars, start, length);
            for(int i=prefix;i<length;i++){
                set_chars[i] = '-';
            }
            return begin + "-" + end;
        }
        return "not implement";
    }
    private void DeclareVars(int[] vars, int bits) {
        for (int i = bits - 1; i >= 0; i--) {
            vars[i] = bdd.createVar();
        }
    }

    public int encodeDstIPPrefixList(List<IPPrefix> ipPrefixList){
        int result = 0;
        for(IPPrefix ipPrefix: ipPrefixList){
            result = bdd.orTo(result, encodeDstIPPrefix(ipPrefix.ip, ipPrefix.prefix));
        }
        return result;
    }

    public int encodeDstIPPrefix(long ipaddr, int prefixlen) {

        int[] ipbin = Utility.CalBinRep(ipaddr, ipBits);
        int[] ipbinprefix = new int[prefixlen];
        for (int k = 0; k < prefixlen; k++) {
            ipbinprefix[k] = ipbin[k + ipBits - prefixlen];
        }
        int entrybdd = EncodePrefix(ipbinprefix, dstIP, ipBits);
        return entrybdd;
    }

    private int EncodePrefix(int[] prefix, int[] vars, int bits) {
        if (prefix.length == 0) {
            return BDDTrue;
        }

        int tempnode = BDDTrue;
        for (int i = 0; i < prefix.length; i++) {
            if (i == 0) {
                tempnode = EncodingVar(vars[bits - prefix.length + i],
                        prefix[i]);
            } else {
                int tempnode2 = EncodingVar(vars[bits - prefix.length + i],
                        prefix[i]);
                int tempnode3 = bdd.ref(bdd.and(tempnode, tempnode2));
                tempnode = tempnode3;
            }
        }
        return tempnode;
    }

    private int EncodingVar(int var, int flag) {
        if (flag == 0) {
            int tempnode = bdd.not(var);
            // no need to ref the negation of a variable.
            // the ref count is already set to maximal
            // aclBDD.ref(tempnode);
            return tempnode;
        }
        if (flag == 1) {
            return var;
        }

        // should not reach here
        System.err.println("flag can only be 0 or 1!");
        return -1;
    }

    public int[] AndInBatch(int [] bddnodes)
    {
        int[] res = new int[bddnodes.length+1];
        res[0] = BDDTrue;
        int tempnode = BDDTrue;
        for(int i = bddnodes.length-1; i >=0 ; i--)
        {
            if(i == bddnodes.length-1)
            {
                tempnode = bddnodes[i];
                bdd.ref(tempnode);
            }else
            {
                if(bddnodes[i] == BDDTrue)
                {
                    // short cut, TRUE does not affect anything
                    continue;
                }
                if(bddnodes[i] == BDDFalse)
                {
                    // short cut, once FALSE, the result is false
                    // the current tempnode is useless now
                    bdd.deref(tempnode);
                    tempnode = BDDFalse;
                    break;
                }
                int tempnode2 = bdd.and(tempnode, bddnodes[i]);
                bdd.ref(tempnode2);
                // do not need current tempnode
                bdd.deref(tempnode);
                //refresh
                tempnode = tempnode2;
            }
            res[bddnodes.length-i] = tempnode;
        }
        return res;
    }

    public int getDstIPCube(int prefix){
        return dstIPField[prefix];
    }
}
