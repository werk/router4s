package com.github.werk.router4s

import scala.reflect.{ClassTag, classTag}
import scala.util.Try

object Router {

    case class PathQuery(
        path : List[String],
        query : List[(String, String)]
    )

    object PathQuery {

        def show(pathQuery : PathQuery) : String = {
            val queryPart = if(pathQuery.query.isEmpty) "" else {
                "?" + pathQuery.query.map{case (n, v) => s"$n=$v"}.mkString("&") // TODO encode
            }
            s"/${pathQuery.path.mkString("/")}$queryPart" // TODO encode
        }

        def read(pathWithQuery : String) : Option[PathQuery] = {
            val (path, query) = pathWithQuery.split('&') match {
                case Array(p) => p -> None
                case Array(p, q) => q -> Some(q)
                case _ => return None
            }
            val pathQuery = PathQuery( // TODO decode
                path = path.split('/').toList.filter(_.nonEmpty),
                query = query.map{q =>
                    val bindings = q.split("&").toList
                    bindings.map{_.split('=') match {
                        case Array(n, v) => n -> v
                        case _ => return None
                    }}
                }.getOrElse(List())
            )
            Some(pathQuery)
        }
    }


    case class Tree[A <: S, S](
        fromPath : PathQuery => Option[A],
        toPath : S => Option[PathQuery],
        prettyPaths : List[String]
    ) {
        def data(pathWithQuery : String) : Option[A] = {
            PathQuery.read(pathWithQuery).flatMap(fromPath)
        }

        def path(data : A) : String = {
            toPath(data).map(PathQuery.show).getOrElse{
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
        read: String => Option[A],
        show: A => Option[String],
        name: String
    )

    val long = Node[Long](
        read = { path : String =>
            for {
                l <- Try(path.toLong).toOption
                if l.toString == path
            } yield l
        },
        show = { l => Some(l.toString) },
        name = "long"
    )

    val int = Node[Int](
        read = { path : String =>
            for {
                l <- Try(path.toInt).toOption
                if l.toString == path
            } yield l
        },
        show = { i => Some(i.toString) },
        name = "int"
    )

}

class Router[S] {

    import Router._

    /** Root */
    def apply[A <: S : ClassTag](a : A, branches : Branch[A, S, S]*) = {
        val tree = Tree[A, S](
            fromPath = { pathQuery =>
                Some(a).filter(_ => pathQuery.path == List())
            },
            toPath = {s =>
                Some(PathQuery(List(), List())).filter(_ => s.getClass == classTag[A].runtimeClass)
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
                fromPath = { pathQuery =>
                    for {
                        init <- Try(pathQuery.path.init).toOption
                        last <- pathQuery.path.lastOption
                        p <- parentTree.fromPath(pathQuery.copy(path = init))
                        if last == name
                    } yield f(p)
                },
                toPath = { s =>
                    if (s.getClass == classTag[B].runtimeClass) {
                        val b = s.asInstanceOf[B]
                        val List(a) = b.productIterator.toList
                        parentTree.toPath(a.asInstanceOf[A]).map(pq => pq.copy(path = pq.path ++ List(name)))
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
                fromPath = { pathQuery =>
                    for {
                        init <- Try(pathQuery.path.init).toOption
                        last <- pathQuery.path.lastOption
                        p <- parentTree.fromPath(pathQuery.copy(path = init))
                        n <- node.read(last)
                    } yield f(n, p)
                },
                toPath = { s =>
                    if (s.getClass == classTag[B].runtimeClass) {
                        val b = s.asInstanceOf[B]
                        val List(n, a) = b.productIterator.toList
                        for {
                            parentPathQuery <- parentTree.toPath(a.asInstanceOf[A])
                            itemPath <- node.show(n.asInstanceOf[N])
                        } yield parentPathQuery.copy(path = parentPathQuery.path ++ List(itemPath))
                    } else {
                        None
                    }
                },
                prettyPaths = parentTree.prettyPaths.map(_ + s" / ${node.name}->${classTag[B].runtimeClass.getSimpleName}")
            )
        })
        branch.apply(subBranches : _*)
    }

    def query[N, A <: S, B <: S with Product : ClassTag](
        parameter : (String, Node[N]),
        f : (N, A) => B,
        subBranches : Branch[B, S, S]*
    ) = {
        val (parameterName, parameterType) = parameter
        val branch = Branch[A, B, S]({ parentTree: Tree[A, S] =>
            Tree[B, S](
                fromPath = { pathQuery =>
                    for {
                        p <- parentTree.fromPath(pathQuery)
                        v <- pathQuery.query.find(_._1 == parameterName).map(_._2)
                        n <- parameterType.read(v)
                    } yield f(n, p)
                },
                toPath = { s =>
                    if (s.getClass == classTag[B].runtimeClass) {
                        val b = s.asInstanceOf[B]
                        val List(n, a) = b.productIterator.toList
                        for {
                            parentPathQuery <- parentTree.toPath(a.asInstanceOf[A])
                            parameterValue <- parameterType.show(n.asInstanceOf[N])
                        } yield parentPathQuery.copy(query = parentPathQuery.query ++ List(parameterName -> parameterValue))
                    } else {
                        None
                    }
                },
                prettyPaths = parentTree.prettyPaths.map(_ + s" ?$parameterName=${parameterType.name}->${classTag[B].runtimeClass.getSimpleName}")
            )
        })
        branch.apply(subBranches : _*)
    }
}
