import IsoPaths.IsoPath

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

object IsoPaths {
    case class IsoPath[P](
        toPath : PartialFunction[P, String],
        fromPath : PartialFunction[String, P]
    )

    def isoNamePath[P](page : P) : IsoPath[P] = {
        val className = page.getClass.getSimpleName
        IsoPath(
            toPath = {case p if p.getClass == page.getClass => className},
            fromPath = {case `className` => page}
        )
    }

    def isoIdPath[P, X <: P{val id : Long}](makePage : Long => X, c : Class[_]) : IsoPath[P] = IsoPath(
        toPath = {case p : X if p.getClass == c => p.id.toString},
        fromPath = {case id if id.forall(_.isDigit) => makePage(id.toLong)}
    )

    def combine[P](isoPaths : List[IsoPath[P]]) : IsoPath[P] = IsoPath[P](
        toPath = isoPaths.map(_.toPath).reduceLeft (_ orElse _),
        fromPath = isoPaths.map(_.fromPath).reduceLeft (_ orElse _)
    )

}

object Pages {
    sealed trait Page
    case object Teams extends Page
    case object TeamsNew extends Page
    case class TeamsTeam(id: Long) extends Page
    case object TeamsTeamFeeds extends Page
    case object TeamsTeamFeedsNew extends Page
    case class TeamsTeamFeedsFeed(id: Long) extends Page
    case object TeamsTeamManageFeed extends Page
}

object Main {
    import IsoPaths._
    import Pages._

    val i : IsoPath[Page] = isoIdPath[Page, TeamsTeam](TeamsTeam.apply, TeamsTeam.getClass)

    val isoPaths = List[IsoPath[Page]](
        isoNamePath(Teams),
        isoNamePath(TeamsNew),
        i
    )
    val isoPath = combine(isoPaths)

    def main(args: Array[String]): Unit = {
        val pages = List(Teams, TeamsNew, TeamsTeam(42))

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
