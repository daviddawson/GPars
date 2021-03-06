// GPars - Groovy Parallel Systems
//
// Copyright © 2008-11  The original author or authors
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

package groovyx.gpars.memoize

/**
 * @author Vaclav Pech
 * Date: Jun 22, 2010
 */
public abstract class AbstractMemoizeTest extends GroovyTestCase {

    public void testCorrectness() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {it * 2}
            Closure mem = buildMemoizeClosure(cl)
            assert 10 == mem(5)
            assert 4 == mem(2)
        }
    }

    abstract Closure buildMemoizeClosure(Closure cl)

    public void testNullParams() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {2}
            Closure mem = cl.gmemoize()
            assert 2 == mem(5)
            assert 2 == mem(2)
            assert 2 == mem(null)
        }
    }

    public void testNullResult() {
        int counter = 0
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {counter++; if (it == 5) return null else return 2}
            Closure mem = cl.gmemoize()
            assert counter == 0
            assertNull mem(5)
            assert counter == 1
            assert 2 == mem(2)
            assert counter == 2
            assertNull mem(5)
            assert 2 == mem(2)
            assert counter == 2
        }
    }

    public void testNoParams() {
        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {-> 2}
            Closure mem = cl.gmemoize()
            assert 2 == mem()
            assert 2 == mem()
        }
    }

    public void testCaching() {
        groovyx.gpars.GParsPool.withPool(5) {
            def flag = false
            Closure cl = {
                flag = true
                it * 2
            }
            Closure mem = cl.gmemoize()
            assert 10 == mem(5)
            assert flag
            flag = false
            assert 4 == mem(2)
            assert flag
            flag = false

            assert 4 == mem(2)
            assert 4 == mem(2)
            assert 10 == mem(5)
            assert !flag

            assert 6 == mem(3)
            assert flag
            flag = false
            assert 6 == mem(3)
            assert !flag
        }
    }

    public void testComplexParameter() {
        def callFlag = []

        groovyx.gpars.GParsPool.withPool(5) {
            Closure cl = {a, b, c ->
                callFlag << true
                c
            }
            Closure mem = cl.gmemoize()
            checkParams(mem, callFlag, [1, 2, 3], 3)
            checkParams(mem, callFlag, [1, 2, 4], 4)
            checkParams(mem, callFlag, [1, [2], 4], 4)
            checkParams(mem, callFlag, [[1: '1'], [2], 4], 4)
            checkParams(mem, callFlag, [[1, 2], 2, 4], 4)
            checkParams(mem, callFlag, [[1, 2], null, 4], 4)
            checkParams(mem, callFlag, [null, null, 4], 4)
            checkParams(mem, callFlag, [null, null, null], null)
            checkParams(mem, callFlag, [null, [null], null], null)

        }
    }

    def checkParams(Closure mem, callFlag, args, desiredResult) {
        assert desiredResult == mem(* args)
        assert !callFlag.empty
        callFlag.clear()
        assert desiredResult == mem(* args)
        assert callFlag.empty
    }
}
