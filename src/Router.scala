import java.net.{URLDecoder, URLEncoder}
import scala.reflect.ClassTag
import scala.util.Try

class Router[TopPage : ClassTag] {

    case class Tree[A <: TopPage] (
        fromPath : List[String] => Option[A],
        toPath : A => Option[List[String]],
        prettyPaths : List[String]
    ) {
        def apply(branch: Branch[A, TopPage])(implicit classTagA : ClassTag[A]) : Tree[TopPage] = {
            this.up.orElse(branch.build(this))
        }

        def up(implicit classTagA : ClassTag[A]) : Tree[TopPage] = {
            // Make it possible to call this method twice as it is wrong to call broaden on a value of PathMap[TopPage]
            // TODO is this correct?
            if(classTagA == scala.reflect.classTag[TopPage]) {
                return this.asInstanceOf[Tree[TopPage]]
            }
            Tree[TopPage](
                fromPath = fromPath,
                toPath = broaden(toPath, classTagA),
                prettyPaths = prettyPaths
            )
        }

        def orElse(that : Tree[A]) : Tree[A] = copy(
            fromPath = orElseO(fromPath, that.fromPath),
            toPath = orElseO(toPath, that.toPath),
            prettyPaths = prettyPaths ++ that.prettyPaths
        )
    }

    case class Branch[A <: TopPage : ClassTag, B <: TopPage : ClassTag] (
        build : Tree[A] => Tree[B]
    ) {
        def apply[C <: TopPage : ClassTag] (child : Branch[B, C]) : Branch[A, TopPage] = {
            val g : Tree[A] => Tree[TopPage] = {a =>
                val b = build(a)
                val c = child.build(b)
                b.up.orElse(c.up)
            }
            Branch[A, TopPage](g)
        }

        // TODO support n children
        def apply[C <: TopPage : ClassTag, D <: TopPage : ClassTag] (child1 : Branch[B, C], child2 : Branch[B, D]) : Branch[A, TopPage] = {
            val g : Tree[A] => Tree[TopPage] = {a =>
                val b = build(a)
                val c = child1.build(b)
                val d = child2.build(b)
                b.up.orElse(c.up.orElse(d.up))
            }
            Branch[A, TopPage](g)
        }
    }

    case class Node[A] (
        fromPath : String => Option[A],
        toPath : A => Option[String],
        name : String
    )

    val long = Node[Long](
        fromPath = { path : String => for {
            l <- Try{path.toLong}.toOption
            if l.toString == path
        } yield l},
        toPath = {l => Some(l.toString)},
        name = "long"
    )

    val string = Node[String](
        fromPath = {path => Some(URLDecoder.decode(path, "UTF-8"))},
        toPath = {s => Some(URLEncoder.encode(s, "UTF-8"))},
        name = "string"
    )

    def constantRoot[A <: TopPage](name : String, a : A) = Tree[A](
        fromPath = { path =>
            Some(a).filter(_ => path == List(name))
        },
        toPath = {_ => Some(List(name))},
        prettyPaths = List(s"'$name'->$a")
    )

    def constant[Page <: TopPage with Product : ClassTag, Parent <: TopPage : ClassTag](name : String, f : Parent => Page) = Branch[Parent, Page]({ parentTree: Tree[Parent] =>
        Tree[Page](
            fromPath = { path: List[String] =>
                for {
                    init <- Try {
                        path.init
                    }.toOption
                    last <- Try {
                        path.last
                    }.toOption
                    p <- parentTree.fromPath(init)
                    if last == name
                } yield f(p)
            },
            toPath = { a =>
                val List(p: Parent) = a.productIterator.toList
                parentTree.toPath(p).map(_ ++ List(name))
            },
            prettyPaths = parentTree.prettyPaths.map(_ + s" / '$name'->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
        )
    })

    def variableRoot[N, Page <: TopPage with Product : ClassTag](node : Node[N], f : N => Page) = Tree[Page](
        fromPath = { path : List[String] =>
            for {
                last <- path.headOption.filter(_ => path.length == 1)
                i <- node.fromPath(last)
            } yield f(i)
        },
        toPath = {page =>
            val List(i : N) = page.productIterator.toList
            node.toPath(i).map{x => List(x)}
        },
        prettyPaths = List(s"${node.name}->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
    )

    def variable[N, Page <: TopPage with Product : ClassTag, Parent <: TopPage : ClassTag](node : Node[N],f : (Parent, N) => Page) = Branch[Parent, Page]({ parentTree: Tree[Parent] =>
        Tree[Page](
            fromPath = { path =>
                for {
                    init <- Try {
                        path.init
                    }.toOption
                    last <- Try {
                        path.last
                    }.toOption
                    p <- parentTree.fromPath(init)
                    i <- node.fromPath(last)
                } yield f(p, i)
            },
            toPath = { page =>
                val List(parent: Parent, i: N) = page.productIterator.toList
                for {
                    parentPath <- parentTree.toPath(parent)
                    itemPath <- node.toPath(i)
                } yield parentPath ++ List(itemPath)
            },
            prettyPaths = parentTree.prettyPaths.map(_ + s" / ${node.name}->${scala.reflect.classTag[Page].runtimeClass.getSimpleName}")
        )
    })

    private def orElseO[A, B, A1 <: A, B1 >: B](f: A => Option[B], g: A1 => Option[B1]): A1 => Option[B1] = {a1 =>
        f(a1).orElse(g(a1))
    }

    private def broaden[A <: C, B, C](f : A => Option[B], classTagA : ClassTag[A]) : C => Option[B] = {a : C =>
        if(a.getClass == classTagA.runtimeClass) f(a.asInstanceOf[A])
        else None
    }
}
