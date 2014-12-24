package mau.test

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

trait MauSpec extends FunSpec with ShouldMatchers with BeforeAndAfterEach with BeforeAndAfterAll {
  def await[T](fut: Future[T], duration: FiniteDuration = 3.seconds): T = Await.result(fut, duration)
}
