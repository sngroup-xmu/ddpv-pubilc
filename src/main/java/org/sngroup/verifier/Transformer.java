

/*
 * This program is free software: you can redistribute it and/or modify it under the terms of
 *  the GNU General Public License as published by the Free Software Foundation, either
 *   version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *   PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 *  program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Authors: Chenyang Huang (Xiamen University) <xmuhcy@stu.xmu.edu.cn>
 *          Qiao Xiang     (Xiamen University) <xiangq27@gmail.com>
 *          Ridi Wen       (Xiamen University) <23020211153973@stu.xmu.edu.cn>
 *          Yuxin Wang     (Xiamen University) <yuxxinwang@gmail.com>
 */

package org.sngroup.verifier;

import org.sngroup.util.Utility;

import java.util.List;
import java.util.Objects;
import java.util.Vector;

public class Transformer {
    public static void main(String[] args) {
        BDDEngine bddEngine1 = new BDDEngine();
        Transformer transformer = new Transformer(bddEngine1);
        int match = bddEngine1.encodeDstIPPrefix(Utility.ip2Int("10.0.0.1"), 32);
        int target = bddEngine1.encodeDstIPPrefix(Utility.ip2Int("20.1.2.3"), 32);
        // NAT： (10.0.0.1) -> (20.1.2.3)
        transformer.register(match, target, 30);

        // transform
        int predicate = bddEngine1.encodeDstIPPrefix(Utility.ip2Int("10.0.0.1"), 30);
        System.out.println("10.0.0.1 encodes to predicate:");
        bddEngine1.printPredicate(predicate);
        System.out.println("10.0.0.1 encodes to predicate after transformation:");
        bddEngine1.printPredicate(transformer.transform(predicate));

        // reverse transform
        int predicate2 = bddEngine1.encodeDstIPPrefix(Utility.ip2Int("20.1.2.3"), 30);
        System.out.println("20.1.2.3 encodes to predicate:");
        bddEngine1.printPredicate(predicate2);
        System.out.println("20.1.2.3 encodes to predicate after reversed transformation:");
        bddEngine1.printPredicate(transformer.inverseTransform(predicate2));
    }
    BDDEngine bddEngine;
    final List<Pair> transformationList;
    public Transformer(BDDEngine bddEngine){
        this.bddEngine = bddEngine;
        transformationList = new Vector<>();
    }


    public void register(int match, int target, int prefix){
        synchronized (transformationList) {
            transformationList.add(new Pair(match, target, bddEngine.getDstIPCube(prefix), this));
        }
    }

    public void remove(int match, int target, int prefix){
        synchronized (transformationList) {

            int index = transformationList.indexOf(new Pair(match, target));
            if (index == -1) {
                System.err.println("tunnel exist");
                return;
            }
            transformationList.remove(index);
        }
    }

    public int transform(int predicate){
        TSBDD bdd = bddEngine.getBDD();
        synchronized (transformationList) {
            for (Pair t : transformationList) {
                int intersection = bdd.and(t.match, predicate);
                if (intersection != 0) {
                    int tmp1 = bdd.ref(bdd.and(predicate, t.notMatch));
//                int tmp2 = bdd.ref(bdd.exists(predicate, t.cube));
//                int tmp3 = bdd.ref(bdd.and(tmp2, t.target));
//                predicate = bdd.ref(bdd.or(tmp1, tmp3));
                    predicate = bdd.ref(bdd.or(tmp1, t.target));
                }
            }
        }
        return predicate;
    }

    public int inverseTransform(int predicate){
        TSBDD bdd = bddEngine.getBDD();
        synchronized (transformationList) {

            for (Pair t : transformationList) {
                int intersection = bdd.and(t.target, predicate);
                if (intersection != 0) {
                    int tmp1 = bdd.ref(bdd.and(predicate, t.notTarget));
//                int tmp2 = bdd.ref(bdd.exists(predicate, t.cube));
//                int tmp3 = bdd.ref(bdd.and(tmp2, t.match));
//                predicate = bdd.ref(bdd.or(tmp1, tmp3));
                    predicate = bdd.ref(bdd.or(tmp1, t.match));
                }
            }
        }
        return predicate;
    }

    public boolean hasTransformation(){
        return !transformationList.isEmpty();
    }
    public static class Pair{
        Transformer transformer;
        int match;
        int notMatch;
        int target;
        int notTarget;

        int cube;
        Pair(int match, int target, int cube, Transformer transformer){
            TSBDD bdd = transformer.bddEngine.getBDD();
            this.target = target;
            this.match = match;
            this.notMatch = bdd.ref(bdd.not(match));
            this.notTarget = bdd.ref(bdd.not(target));
            this.transformer = transformer;
            this.cube = cube;
        }
        Pair(int match, int target){
            this.target = target;
            this.match = match;
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pair pair = (Pair) o;
            return match == pair.match && target == pair.target;
        }

        @Override
        public int hashCode() {
            return Objects.hash(match, target);
        }
    }
}
