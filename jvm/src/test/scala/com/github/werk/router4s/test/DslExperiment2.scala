package com.github.werk.router4s.test

import scala.reflect.ClassTag

object DslExperiment2 {

    trait Path0 {
        def bind[P <: Product : ClassTag](page : P) : BoundPath
    }

    trait Path1[V1] {
        def bind[O <: Product : ClassTag](makePage : V1 => O) : BoundPathO[O]
        def bind[I, O <: Product : ClassTag](makePage : (V1, I) => O) : BoundPathIO[I, O]
    }

    trait Path2[V1, V2] {
        def bind[O <: Product : ClassTag](makePage : (V1, V2) => O) : BoundPathO[O]
        def bind[I, O <: Product : ClassTag](makePage : (V1, V2, I) => O) : BoundPathIO[I, O]
    }


    trait BoundPath {
        def extend(p : BoundPath) : BoundPath
        def extend[O](p : BoundPathO[O]) : BoundPathO[O]
    }
    trait BoundPathO[O] {
        def extend(p : BoundPath) : BoundPathO[O]
        def extend[O2](p : BoundPathIO[O, O2]) : BoundPathO[O2]
    }
    trait BoundPathIO[I, O] {
        def extend(p : BoundPath) : BoundPathIO[I, O]
        def extend[O2](p : BoundPathIO[O, O2]) : BoundPathIO[I, O2]

        def children[O2](rs : RouterIO[O, O2]*) : RouterIO[I, O2]
    }

    trait RouterIO[I, O]

}
