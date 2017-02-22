
/*
    /teams                             <-> Teams
    /teams/new                         <-> TeamNew
    /teams/320749                      <-> Team(320749)
    /teams/320749/feeds                <-> TeamFeeds(320749)
    /teams/320749/manage-feeds         <-> TeamManageFeeds(320749)
    /teams/320749/feeds/new            <-> TeamFeedsNew(320749)
    /teams/320749/feeds/840327         <-> TeamFeed(320749, 840327)

    /teams                             <-> Teams((), None)
    /teams/new                         <-> TeamNew
    /teams/320749                      <-> Team(320749)
    /teams/320749/feeds                <-> TeamFeeds(320749)
    /teams/320749/feeds/new            <-> TeamFeedsNew(320749)
    /teams/320749/feeds/840327         <-> TeamFeed(320749, 840327)
    /teams/320749/manage-feeds         <-> TeamManageFeeds(320749)
 */
object Paths {

    sealed abstract class Page[+Parent <: Page[_]](val parent : Parent) {
        def toPath : String
    }

    sealed abstract class IdPage[+Parent <: Page[_]](override val parent : Parent) extends Page(parent) {
        val id : Long
        def toPath : String = (if(parent == null) "" else parent.toPath) + "/" + id
    }

    sealed abstract class NamePage[+Parent <: Page[_]](override val parent : Parent) extends Page(parent) {
        def toPath : String = (if(parent == null) "" else parent.toPath) + "/" + this.getClass.getSimpleName
    }

    case class Teams() extends NamePage(null) { outer =>
        case class New() extends NamePage(outer)
        case class Team(id : Long) extends IdPage(outer) { outer =>
            case class Feeds() extends NamePage(outer) { outer =>
                case class New() extends NamePage(outer)
                case class Feed(id : Long) extends IdPage(outer)
            }
            case class ManageFeed() extends NamePage(outer)
        }
    }


    def toPath(page : Page[_]) : String = page match {
        case Teams() => "/teams"
        case p : Teams#New => toPath(p.parent) + "/new"
        case p : Teams#Team => toPath(p.parent) + "/" + p.id
        case p : Teams#Team#Feeds => toPath(p.parent) + "/feeds"
        case p : Teams#Team#Feeds#New => toPath(p.parent) + "/new"
        case p : Teams#Team#Feeds#Feed => toPath(p.parent) + "/" + p.id
        case p : Teams#Team#ManageFeed => toPath(p.parent) + "/manage-feeds"
    }

    def main(args: Array[String]): Unit = {
        val feed = Teams().Team(42).Feeds().Feed(1337)
        println(feed.getClass + " " + feed)
        println(toPath(feed))
        println(feed.toPath)

        Separate.test()
    }
}

object ReadTest {
    trait Read[A] { def read(s: String): A }

    case class Person(name: String, age: Int)

    implicit object ReadPerson extends Read[Person] {
        def read(s: String): Person = Person("John", 42)
    }

    def readList[A: Read] (ss: List[String]): List[A] = {
        val r = implicitly[Read[A]]
        ss.map(r.read)
    }

    def read[A: Read](s: String): A = implicitly[Read[A]].read(s)

    val person: Person = read[Person]("Person(Bob,42)")

    sealed trait Fruit
    case object Apple extends Fruit
    case class Orange(name : String) extends Fruit

    implicit object ReadFruit extends Read[Fruit] {
        def read(s: String): Fruit = ???
    }
}

object Separate {
    sealed trait Page
    case object Teams extends Page
    case object TeamsNew extends Page
    case class TeamsTeam(id : Long) extends Page
    case object TeamsTeamFeeds extends Page
    case object TeamsTeamFeedsNew extends Page
    case class TeamsTeamFeedsFeed(id : Long) extends Page
    case object TeamsTeamManageFeed extends Page

    case class IsoPath(
        toPath : PartialFunction[Page, String],
        fromPath : PartialFunction[String, Page]
    )

    def nameIso[P <: Page](page : P) : IsoPath = {
        val className = page.getClass.getSimpleName
        IsoPath(
            toPath = {case p : P if p.getClass == page.getClass => className},
            fromPath = {case `className` => page}
        )
    }

    def idIso[P <: Page {val id : Long}](makePage : Long => P) : IsoPath = IsoPath(
        toPath = {case p : P => p.id.toString},
        fromPath = {case "TODO" => makePage(42)}
    )

    val f = TeamsTeam.apply _

    val isoPaths = List[IsoPath](
        nameIso(Teams),
        nameIso(TeamsNew)
        //idIso[TeamsTeam](f)
    )

    val isoPath = IsoPath(
        toPath = isoPaths.map(_.toPath).reduceLeft (_ orElse _),
        fromPath = isoPaths.map(_.fromPath).reduceLeft (_ orElse _)
    )

    def test() {
        println()

        println(nameIso(Teams).toPath.isDefinedAt(Teams))
        println(nameIso(TeamsNew).toPath.isDefinedAt(Teams))

        println(isoPath.toPath.isDefinedAt(Teams))
        println(isoPath.toPath.isDefinedAt(TeamsNew))

        val pages = List(Teams, TeamsNew)
        pages.foreach{page =>
            println()
            println(s"Testing page $page")
            val path = isoPath.toPath(page)
            println(s"toPath: $path")
            val back = isoPath.fromPath(path)
            println(s"fromPath: $back")
        }

    }


}
