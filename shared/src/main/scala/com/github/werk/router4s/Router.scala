package com.github.werk.router4s

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

object Router {

    case class Tree[A <: S, S](
        fromPath: List[String] => Option[A],
        toPath: S => Option[List[String]],
        prettyPaths: List[String]
    ) {
        def data(path : String) : Option[A] = {
            fromPath(path.split('/').toList.filter(_.nonEmpty))
        }

        def path(data : A) : String = {
            toPath(data).map("/" + _.mkString("/")).getOrElse{
                throw new RuntimeException(s"Router is unexhaustive. Failed on $data")
            }
        }

        def apply(branches: Branch[A, S, S]*) : Tree[S, S] = {
            val trees = this.up +: branches.map(_.build(this))
            trees.reduceLeft(_.orElse(_))
        }

        def up : Tree[S, S] = this.asInstanceOf[Tree[S, S]]

        def orElse(that: Tree[A, S]): Tree[A, S] = copy(
            fromPath = orElseO(fromPath, that.fromPath),
            toPath = orElseO(toPath, that.toPath),
            prettyPaths = prettyPaths ++ that.prettyPaths
        )
    }

    private def orElseO[A, B, A1 <: A](f: A => Option[B], g: A1 => Option[B]): A1 => Option[B] = { a1 =>
        f(a1).orElse(g(a1))
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
        fromPath = { path : String =>
            for {
                l <- Try(path.toLong).toOption
                if l.toString == path
            } yield l
        },
        toPath = { l => Some(l.toString) },
        name = "long"
    )

    val int = Node[Int](
        fromPath = { path : String =>
            for {
                l <- Try(path.toInt).toOption
                if l.toString == path
            } yield l
        },
        toPath = { i => Some(i.toString) },
        name = "int"
    )

}

class Router[S] {

    import Router._

    /** Root */
    def apply[A <: S : ClassTag](a : A, branches : Branch[A, S, S]*) = {
        val tree = Tree[A, S](
            fromPath = { path =>
                Some(a).filter(_ => path == List())
            },
            toPath = {s =>
                Some(List()).filter(_ => s.getClass == classTag[A].runtimeClass)
            },
            prettyPaths = List(s"$a")
        )
        tree.apply(branches : _*)
    }

    /** Constant sub-path */
    def apply[A <: S, B <: S with Product : ClassTag](
        name : String,
        f : A => B,
        subBranches: Branch[B, S, S]*
    ) = {
        val branch = Branch[A, B, S]({ parentTree: Tree[A, S] =>
            Tree[B, S](
                fromPath = { path: List[String] =>
                    for {
                        init <- Try(path.init).toOption
                        last <- path.lastOption
                        p <- parentTree.fromPath(init)
                        if last == name
                    } yield f(p)
                },
                toPath = { s =>
                    if (s.getClass == classTag[B].runtimeClass) {
                        val b = s.asInstanceOf[B]
                        val List(a) = b.productIterator.toList
                        parentTree.toPath(a.asInstanceOf[A]).map(_ ++ List(name))
                    } else {
                        None
                    }
                },
                prettyPaths = parentTree.prettyPaths.map(_ + s" / '$name'->${scala.reflect.classTag[B].runtimeClass.getSimpleName}")
            )
        })
        branch.apply(subBranches : _*)
    }

    /** Variable sub-path */
    def apply[N, A <: S, B <: S with Product : ClassTag](
        node : Node[N],
        f : (N, A) => B,
        subBranches: Branch[B, S, S]*
    ) = {
        val branch = Branch[A, B, S]({ parentTree: Tree[A, S] =>
            Tree[B, S](
                fromPath = { path =>
                    for {
                        init <- Try(path.init).toOption
                        last <- path.lastOption
                        p <- parentTree.fromPath(init)
                        n <- node.fromPath(last)
                    } yield f(n, p)
                },
                toPath = { s =>
                    if (s.getClass == classTag[B].runtimeClass) {
                        val b = s.asInstanceOf[B]
                        val List(n, a) = b.productIterator.toList
                        for {
                            parentPath <- parentTree.toPath(a.asInstanceOf[A])
                            itemPath <- node.toPath(n.asInstanceOf[N])
                        } yield parentPath ++ List(itemPath)
                    } else {
                        None
                    }
                },
                prettyPaths = parentTree.prettyPaths.map(_ + s" / ${node.name}->${classTag[B].runtimeClass.getSimpleName}")
            )
        })
        branch.apply(subBranches : _*)
    }
}
