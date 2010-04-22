// GPars (formerly GParallelizer)
//
// Copyright © 2008-10  The original author or authors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package groovyx.gpars.samples

import static groovyx.gpars.GParsPool.withPool

/**
 * Demo the possible use of Groovy immutable support for the classical
 * account money transfer problem.
 * Account becomes immutable without knowing anything about synchronization.
 * However, since transferTo() returns two new Accounts, these references
 * must be assigned in one atomic action. This is what AtomicPair cares for.
 * Locks are restricted to the reference updates only, while potentially
 * long-running business tasks (longer than transferTo) can run lock-free.
 * @author Dierk König
 */
@Immutable class ImmutableAccount {
    int balance = 0

    ImmutableAccount credit(int add) {
        new ImmutableAccount(balance + add)
    }

    /** @return list of new "this" and new target account  */
    List<ImmutableAccount> transferTo(ImmutableAccount target, int amount) {
        [credit(-amount), target.credit(amount)]
    }
}

class AtomicPair {
    private List safe
    AtomicPair(a,b) {
        safe = [a,b].asImmutable()
    }
    synchronized List getAb() { safe }
    synchronized void setAb(List ab) {
        safe = ab.asImmutable()
    }
}

def pair = new AtomicPair(new ImmutableAccount(0), new ImmutableAccount(0))

withPool(50) {
    (1..1000).eachParallel {
        assert [0, 0] == pair.ab.balance
        def (a,b) = pair.ab                 // tricky bit: does it need to be fetched in advance (?)
        (a,b)     = a.transferTo(b,  it)    // store the refs for the return transfer only locally
        pair.ab   = a.transferTo(b, -it)    // update the safe pair to make new refs visible for other tasks
    }
}
assert [0, 0] == pair.ab.balance