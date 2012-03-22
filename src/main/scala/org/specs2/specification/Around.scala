package org.specs2
package specification

import execute._

/**
 * The Around trait can be inherited by classes which will
 * execute some code inside the around method provided by the context.
 * 
 * This can be used for example to execute some code inside a webapp session
 * 
 * @see Example to understand why the type T must : Executable
 */
trait Around extends Context { outer =>

  def around[T : Executable](t: =>T): Result
  def apply[T : Executable](a: =>T) = around(a)
  
  /** compose the actions of 2 Around traits */
  def compose(a: Around): Around = new Around {
    def around[T : Executable](t: =>T): Result = {
      a.around(outer.around(t))
    }
  }

  /** sequence the actions of 2 Around traits */
  def then(a: Around): Around = new Around {
    def around[T : Executable](t: =>T): Result = {
      outer.around(a.around(t))
    }
  }
}

