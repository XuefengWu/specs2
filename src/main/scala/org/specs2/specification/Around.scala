package org.specs2
package specification

import execute._

/**
 * The Around trait can be inherited by classes which will
 * execute some code inside the around method provided by the context.
 * 
 * This can be used for example to execute some code inside a webapp session
 * 
 * @see Example to understand why the type T must <% Result
 */
trait Around extends Context { outer =>

  def around(t: =>Result): Result
  def apply(a: =>Result) = around(a)
  
  /** compose the actions of 2 Around traits */
  def compose(a: Around): Around = new Around {
    def around(t: =>Result): Result = {
      a.around(outer.around(t))
    }
  }

  /** sequence the actions of 2 Around traits */
  def then(a: Around): Around = new Around {
    def around(t: =>Result): Result = {
      outer.around(a.around(t))
    }
  }
}

