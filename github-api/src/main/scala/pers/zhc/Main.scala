package pers.zhc

import com.google.gson.{Gson, JsonArray}
import pers.zhc.util.IOUtils

import java.io.{ByteArrayOutputStream, File, FileOutputStream}
import java.net.{HttpURLConnection, URL, URLConnection}
import java.util.regex.Pattern
import java.util.{Calendar, Date, TimeZone}
import scala.jdk.CollectionConverters.IterableHasAsScala

/** @author
  *   bczhc
  */
object Main {
  val USER = "bczhc"
  var PAT: String = _
  val GSON = new Gson()
  def main(args: Array[String]): Unit = {

    val pat = Option(System.getenv("GITHUB_PAT"))
    if (pat.isEmpty) {
      println("Please set `GITHUB_PAT` env")
      return
    }
    PAT = pat.get

    val repoExcludeList = Option(System.getenv("REPO_EXCLUDE"))
      .getOrElse("")
      .split(",")
      .map(_.trim)
      .toList

    val repos = requestRepos()
      .filterNot({ it => repoExcludeList.contains(it.name) })
      .toList

    writeReposToFile(repos)
  }

  def writeReposToFile(repos: Iterable[Repository]): Unit = {
    val file = new File("./repos")
    val os = new FileOutputStream(file)

    repos.foreach { repo =>
      os.write(repo.name.getBytes)
      os.write('\n')
    }

    os.flush()
    os.close()
  }

  def requestRepos(): Repos = {
    val repoRequest = new RepoRequest(USER)
    repoRequest
      .map({ it => repoRequest.parse(it) })
      .toArray
      .flatten
      .filter(!_.`private`)
  }

  class UnreachableError extends Error
  def unreachable[T](): T = {
    throw new UnreachableError()
  }

  case class Repository(name: String, `private`: Boolean)
  type Repos = Array[Repository]

  def addPatHeader(connection: URLConnection): Unit = {
    connection.addRequestProperty("authorization", s"Bearer $PAT")
  }

  def readConnectionToString(
      connection: URLConnection,
      charset: String = "UTF-8"
  ): String = {
    val is = connection.getInputStream
    val os = new ByteArrayOutputStream()
    IOUtils.streamWrite(is, os)
    is.close()
    os.toString(charset)
  }

  trait MultiPage extends Iterator[String] {

    /** @param page
      *   begin from 0
      * @return
      *   response
      */
    def requestPage(page: Int): Option[String]
    private var lastRequest: Option[String] = _
    private var page = 0

    override def hasNext: Boolean = {
      lastRequest = requestPage(page)
      page += 1
      lastRequest.nonEmpty
    }

    override def next(): String = {
      lastRequest.get
    }
  }

  trait ParseResponse {
    type Parsed
    def parse(read: String): Parsed
  }

  class RepoRequest(private val user: String)
      extends MultiPage
      with ParseResponse {
    val URL_PATTERN =
      s"https://api.github.com/users/$user/repos?page=%d&per_page=100"
    override type Parsed = Repos

    private def urlAt(page: Int): URL = {
      new URL(URL_PATTERN.format(page + 1))
    }

    override def requestPage(page: Int): Option[String] = {
      val url = urlAt(page)
      val conn = url.openConnection()
      val read = readConnectionToString(conn)

      if (checkPageEnd(read)) {
        None
      } else {
        Some(read)
      }
    }

    override def parse(read: String): Parsed = {
      GSON.fromJson(read, classOf[Repos])
    }
  }

  case class Commit(author: String, authorTime: Int)
  type Commits = Array[Commit]

  class CommitRequest(repo: Repository, user: String)
      extends MultiPage
      with ParseResponse {
    val URL_PATTERN =
      s"https://api.github.com/repos/$user/${repo.name}/commits?page=%d&per_page=100"
    override type Parsed = Commits

    private def urlAt(pageIndex: Int): URL = new URL(
      URL_PATTERN.format(pageIndex + 1)
    )

    override def requestPage(page: Int): Option[String] = {
      val conn = urlAt(page).openConnection().asInstanceOf[HttpURLConnection]
      addPatHeader(conn)
      require(conn.getResponseCode == 200)

      val read = readConnectionToString(conn)
      if (checkPageEnd(read)) {
        None
      } else {
        Some(read)
      }
    }

    override def parse(read: String): Parsed = {
      val jsonArray = GSON.fromJson(read, classOf[JsonArray])
      jsonArray.asScala
        .map({ it =>
          val authorName =
            it.getAsJsonObject
              .getAsJsonObject("commit")
              .getAsJsonObject("author")
              .get("name")
              .getAsString
          val authorTime =
            it.getAsJsonObject
              .getAsJsonObject("commit")
              .getAsJsonObject("author")
              .get("date")
              .getAsString
          val parsedAuthorTime = parseIso8601(authorTime)
          Commit(authorName, (parsedAuthorTime.getTime / 1000).toInt)
        })
        .toArray
    }
  }

  def checkPageEnd(read: String): Boolean = {
    GSON.fromJson(read, classOf[Array[Any]]).length == 0
  }

  private val ISO8601_PATTERN = Pattern.compile(
    "^([0-9]{4})-([0-9]{2})-([0-9]{2})T([0-9]{2}):([0-9]{2}):([0-9]{2})Z$"
  )
  def parseIso8601(src: String): Date = {
    val matcher = ISO8601_PATTERN.matcher(src)
    require(matcher.find())

    val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTF"))
    calendar.set(
      matcher.group(1).toInt,
      matcher.group(2).toInt,
      matcher.group(3).toInt,
      matcher.group(4).toInt,
      matcher.group(5).toInt,
      matcher.group(6).toInt
    )
    calendar.getTime
  }

  def requestCommits(repo: Repository, user: String): Commits = {
    val commits = new CommitRequest(repo, user)
    commits.map({ it => commits.parse(it) }).toArray.flatten
  }

  def writeCommitsATime(commits: Iterable[Commit]): Unit = {
    val file = new File("./atimes")
    val os = new FileOutputStream(file)

    commits.foreach { commit =>
      os.write(commit.authorTime.toString.getBytes)
      os.write('\n')
    }

    os.flush()
    os.close()
  }
}
