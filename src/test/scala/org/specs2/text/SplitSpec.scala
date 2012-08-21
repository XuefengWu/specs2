package org.specs2
package text

import Split._

class SplitSpec extends mutable.Specification {
  "a command line can be splitted" >> {
    "around spaces" >> {
      "this is hello world".splitQuoted === Seq("this", "is", "hello", "world")
    }
    "around double quotes" >> {
      "this \"is hello\" world".splitQuoted === Seq("this", "is hello", "world")
    }
    "around dashed keywords" >> {
      "nocolor -include hello world -with me -- other arguments".splitDashed(Seq("include", "With")) === Seq("nocolor", "include", "hello world", "with", "me", "other", "arguments")
    }
  }
}