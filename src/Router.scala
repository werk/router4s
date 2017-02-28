import java.net.{URLDecoder, URLEncoder}
import scala.reflect.{ClassTag, classTag}
import scala.util.Try


object Router {

    case class Tree[A <: S, S](
        fromPath: List[String] => Option[A],
        toPath: A => Option[List[String]],
        prettyPaths: List[String],
        classTagA: Option[ClassTag[A]]
    ) {
        def apply(branches: Branch[A, S, S]*) : Tree[S, S] = {
            val trees = this.up +: branches.map(_.build(this))
            trees.reduceLeft(_.orElse(_))
        }

        def up: Tree[S, S] = {
            // Make it possible to call this method twice as it is wrong to call broaden on a value of PathMap[TopPage]
            // TODO is this correct?
            classTagA match {
                case Some(tag) =>
                    Tree[S, S](
                        fromPath = fromPath,
                        toPath = broaden(toPath, tag),
                        prettyPaths = prettyPaths,
                        None
                    )
                case None =>
                    this.asInstanceOf[Tree[S, S]]
            }
        }

        def orElse(that: Tree[A, S]): Tree[A, S] = copy(
            fromPath = orElseO(fromPath, that.fromPath),
            toPath = orElseO(toPath, that.toPath),
            prettyPaths = prettyPaths ++ that.prettyPaths
        )
    }

    case class Branch[A <: S, B <: S, S](
        build: Tree[A, S] => Tree[B, S]
    ) {
        def apply(children: Branch[B, _ <: S, S]*): Branch[A, S, S] = {
            val g: Tree[A, S] => Tree[S, S] = { a =>
                val b = build(a)
                val trees = b.up +: children.map(_.build(b).up)
                trees.reduceLeft(_.orElse(_))
            }
            Branch[A, S, S](g)
        }
    }

    case class Node[A](
        fromPath: String => Option[A],
        toPath: A => Option[String],
        name: String
    )

    val long = Node[Long](
        fromPath = { path: String =>
            for {
                l <- Try {
                    path.toLong
                }.toOption
                if l.toString == path
            } yield l
        },
        toPath = { l => Some(l.toString) },
        name = "long"
    )

    val string = Node[String](
        fromPath = { path => Some(URLDecoder.decode(path, "UTF-8")) },
        toPath = { s => Some(URLEncoder.encode(s, "UTF-8")) },
        name = "string"
    )

    private def orElseO[A, B, A1 <: A, B1 >: B](f: A => Option[B], g: A1 => Option[B1]): A1 => Option[B1] = { a1 =>
        f(a1).orElse(g(a1))
    }

    private def broaden[A <: C, B, C](f: A => Option[B], classTagA: ClassTag[A]): C => Option[B] = { a: C =>
        if (a.getClass == classTagA.runtimeClass) f(a.asInstanceOf[A])
        else None
    }
}

class Router[S] {

    import Router._

    /** Root */
    def apply[A <: S : ClassTag](a : A) = Tree[A, S](
        fromPath = { path =>
            Some(a).filter(_ => path == List())
        },
        toPath = {_ => Some(List())},
        prettyPaths = List(s"$a"),
        classTagA = Some(classTag[A])
    )

    /** Constant sub-path */
    def apply[A <: S : ClassTag, B <: S with Product : ClassTag](name : String, f : A => B) = Branch[A, B, S]({ parentTree: Tree[A, S] =>
        Tree[B, S](
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
                val List(p: A) = a.productIterator.toList
                parentTree.toPath(p).map(_ ++ List(name))
            },
            prettyPaths = parentTree.prettyPaths.map(_ + s" / '$name'->${scala.reflect.classTag[B].runtimeClass.getSimpleName}"),
            classTagA = Some(classTag[B])
        )
    })

    /** Variable sub-path */
    def apply[N, A <: S : ClassTag, B <: S with Product : ClassTag](node : Node[N],f : (N, A) => B) = Branch[A, B, S]({ parentTree: Tree[A, S] =>
        Tree[B, S](
            fromPath = { path =>
                for {
                    init <- Try {
                        path.init
                    }.toOption
                    last <- Try {
                        path.last
                    }.toOption
                    p <- parentTree.fromPath(init)
                    n <- node.fromPath(last)
                } yield f(n, p)
            },
            toPath = { page =>
                val List(i, parent : A) = page.productIterator.toList
                for {
                    parentPath <- parentTree.toPath(parent)
                    itemPath <- node.toPath(i.asInstanceOf[N])
                } yield parentPath ++ List(itemPath)
            },
            prettyPaths = parentTree.prettyPaths.map(_ + s" / ${node.name}->${classTag[B].runtimeClass.getSimpleName}"),
            classTagA = Some(classTag[B])
        )
    })
}
