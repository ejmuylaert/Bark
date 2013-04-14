package nl.spotdog.bark.protocol

import shapeless._

import akka.util.{ ByteIterator, ByteStringBuilder, ByteString }

import nl.spotdog.bark.protocol._
import nl.spotdog.bark.protocol.ETFTypes._

import akka.io.PipelineContext

trait Request {
  def module: Atom
  def functionName: Atom
  def arguments: Product
}

trait Response

trait FailedResponse extends Response

object Request {

  /* 
   * Call: Send a request to a server, waiting for a immediate (blocking) response, best used in CPU bound services. 
   * 
   * (`call, `persons, `fetch, ("gideondk")))
   * 
   */
  case class Call[T <: Product](module: Atom, functionName: Atom, arguments: T) extends Request

  /* 
   * Cast: Send a request to a server, waiting for a immediate reply but not a response (fire and forget)
   * 
   * (`cast, `persons, `remove, ("gideondk")))
   *   
   */
  case class Cast[T <: Product](module: Atom, functionName: Atom, arguments: T) extends Request
}

object Response {
  /* 
   * Reply: Send back to the client with the resulting value
   * 
   * (`reply, ("Gideon", "de Kok"))
   * 
   * */
  case class Reply(value: ByteString) extends Response

  /* 
   * NoReply: Send back to the client in case of a "cast" request
   * 
   * (`noreply)
   * 
   * */
  case class NoReply() extends Response

  /* 
   * Error: Send back to the client in case of an error
   * 
   * Error types: protocol, server, user, and proxy (BERT-RPC style)
   * ('error, `server, 2, "UnknownFunction", "function 'collect' not found on module 'logs'", [""])
   * 
   * */
  case class Error(errorType: Atom, errorCode: Int, errorClass: String, errorDetail: String, backtrace: List[String]) extends FailedResponse
}

object BarkMessaging extends ETFConverters with TupleConverters {
  import HeaderFunctions._

  trait HasByteOrder extends PipelineContext {
    def byteOrder: java.nio.ByteOrder
  }

  /* Request */
  implicit def callConverter[T <: Product](implicit c1: ETFConverter[T]) = new ETFConverter[Request.Call[T]] {
    def write(o: Request.Call[T]) = {
      val callTpl = Request.Call.unapply(o).get
      tuple4Converter[Atom, Atom, Atom, T].write(Atom("call"), callTpl._1, callTpl._2, callTpl._3)
    }

    def readFromIterator(iter: ByteIterator): Request.Call[T] = {
      val tpl = tuple4Converter[Atom, Atom, Atom, T].readFromIterator(iter)
      Request.Call(tpl._2, tpl._3, tpl._4)
    }
  }

  implicit def castConverter[T <: Product](implicit c1: ETFConverter[T]) = new ETFConverter[Request.Cast[T]] {
    def write(o: Request.Cast[T]) = {
      val callTpl = Request.Cast.unapply(o).get
      tuple4Converter[Atom, Atom, Atom, T].write(Atom("cast"), callTpl._1, callTpl._2, callTpl._3)
    }

    def readFromIterator(iter: ByteIterator): Request.Cast[T] = {
      val tpl = tuple4Converter[Atom, Atom, Atom, T].readFromIterator(iter)
      Request.Cast(tpl._2, tpl._3, tpl._4)
    }
  }

  /* Response */

  implicit def replyConverter = new ETFConverter[Response.Reply] {
    def write(o: Response.Reply) = {
      val builder = new ByteStringBuilder
      builder.putByte(MAGIC)
      builder.putByte(SMALL_TUPLE)
      builder.putByte(2.toByte)

      builder ++= AtomConverter.write(Atom("reply"))
      builder ++= o.value
      builder.result
    }

    def readFromIterator(iter: ByteIterator): Response.Reply = {
      checkMagic(iter.getByte)
      checkSignature(SMALL_TUPLE, iter.getByte)
      val size = iter.getByte
      val v1 = AtomConverter.readFromIterator(iter)
      Response.Reply(iter.toByteString)
    }
  }

  implicit def noReplyConverter = new ETFConverter[Response.NoReply] {
    def write(o: Response.NoReply) = {
      tuple1Converter[Atom].write(Tuple1(Atom("noreply")))
    }

    def readFromIterator(iter: ByteIterator): Response.NoReply = {
      val tpl = tuple1Converter[Atom].readFromIterator(iter)
      Response.NoReply()
    }
  }

  implicit def errorConverter = new ETFConverter[Response.Error] {
    def write(o: Response.Error) = {
      tuple6Converter[Atom, Atom, Int, String, String, List[String]].write((Atom("error"), o.errorType, o.errorCode, o.errorClass, o.errorDetail, o.backtrace))
    }

    def readFromIterator(iter: ByteIterator): Response.Error = {
      val tpl = tuple6Converter[Atom, Atom, Int, String, String, List[String]].readFromIterator(iter)
      Response.Error(tpl._2, tpl._3, tpl._4, tpl._5, tpl._6)
    }
  }
}