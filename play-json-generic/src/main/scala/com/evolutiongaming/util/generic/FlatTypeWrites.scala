package com.evolutiongaming.util.generic
import play.api.libs.json._
import shapeless._
import shapeless.labelled.FieldType

trait FlatTypeWrites[A] extends Writes[A] {
  override def writes(o: A): JsObject
}

object FlatTypeWrites {

  def apply[A](implicit encode: FlatTypeWrites[A]): Writes[A] = new Writes[A] {
    def writes(o: A): JsValue = encode writes o
  }

  def create[A](f: A => JsObject): FlatTypeWrites[A] = new FlatTypeWrites[A] {
    override def writes(o: A): JsObject = f(o)
  }

  implicit def cnilWrites: FlatTypeWrites[CNil] = create[CNil] { _ =>
    sys.error("Cannot encode CNil")
  }

  implicit def cconsWrites[Key <: Symbol, Head, Tail <: Coproduct](implicit
      key: Witness.Aux[Key],
      headWrites: OWrites[Head],
      tailWrites: FlatTypeWrites[Tail]): FlatTypeWrites[FieldType[Key, Head] :+: Tail] =
    create[FieldType[Key, Head] :+: Tail] {
      _.eliminate(
        head => Json.obj("type" -> s"${ key.value.name }") ++ (headWrites writes head),
        tail => tailWrites writes tail
      )
    }

  implicit def flatTypeWrites[A, Repr <: Coproduct](implicit
      gen: LabelledGeneric.Aux[A, Repr],
      writes: FlatTypeWrites[Repr]
  ): FlatTypeWrites[A] = create[A] { a => writes writes gen.to(a) }
}