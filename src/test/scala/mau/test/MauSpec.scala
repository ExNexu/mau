package mau.test

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future

import org.scalatest.FunSpec
import org.scalatest.ShouldMatchers

trait MauSpec extends FunSpec with ShouldMatchers {
  def await[T](fut: Future[T], duration: FiniteDuration = 3.seconds): T = Await.result(fut, duration)
}
