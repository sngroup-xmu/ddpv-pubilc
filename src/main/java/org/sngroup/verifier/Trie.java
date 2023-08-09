package org.sngroup.verifier;

import org.sngroup.util.Rule;

import java.util.ArrayList;

class TrieNode {
    ArrayList<Rule> rules;
    TrieNode left, right;

    public TrieNode() {
        rules = new ArrayList<>();
        left = right = null;
    }

    public TrieNode getNext(int flag) {
        if (flag == 0) {
            if (this.left == null) {
                this.left = new TrieNode();
            }
            return this.left;
        } else {
            if (this.right == null) {
                this.right = new TrieNode();
            }
            return this.right;
        }
    }

    public void add(Rule rule) {
        this.rules.add(rule);
    }

    public ArrayList<Rule> getRules() {
        return this.rules;
    }

    public void explore(ArrayList<Rule> ret) {
        if (this.left != null) this.left.explore(ret);
        if (this.right != null) this.right.explore(ret);
        ret.addAll(this.getRules());
    }
}

public class Trie {
    TrieNode root;

    public Trie() {
        this.root = new TrieNode();
    }

    public ArrayList<Rule> addAndGetAllOverlappingWith(Rule rule) {
        TrieNode t = this.root;

        ArrayList<Rule> ret = new ArrayList<>(t.getRules());

        long dstIp = rule.ip;
        long bit = 1L << 31;
        for (int i = 0; i < rule.getPriority(); i++) {

            boolean flag = (bit & dstIp) == 0;
            t = t.getNext(flag ? 0 : 1);
            bit >>=1;
            ret.addAll(t.getRules());
        }

        t.explore(ret);
        t.add(rule);
        return ret;
    }

}
