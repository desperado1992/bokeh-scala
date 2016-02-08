package io.continuum.bokeh

import play.api.libs.json.{JsValue,Writes}

case class Selected0d(indices: List[Int] = Nil, glyph: Option[Glyph] = None)
case class Selected1d(indices: List[Int] = Nil)
case class Selected2d(indices: List[List[Int]] = Nil)

case class Selected(`0d`: Selected0d = Selected0d(),
                    `1d`: Selected1d = Selected1d(),
                    `2d`: Selected2d = Selected2d())

@model abstract class DataSource extends Model {
    object column_names extends Field[List[String]]
    object selected extends Field[Selected]
    object callback extends Field[Callback]
}

@model class ColumnDataSource extends DataSource { source =>
    final override val typeName = "ColumnDataSource"

    object data extends Field[Map[Symbol, JsValue]]

    class Column[M[_]: ArrayLike, T:Writes]
            (val name: Symbol, private var _value: M[T])
            (implicit fmt: Writes[M[T]]) {
        this := _value

        def value: M[T] = _value // TODO: fmt.reads(source.data.value(name))
        def :=(value: M[T]): Unit = data <<= (_ + (name -> fmt.writes(_value)))
    }

    def column[M[_], T](value: M[T]): Column[M, T] = macro ColumnMacro.columnImpl[M, T]
}

private[bokeh] object ColumnMacro {
    import scala.reflect.macros.blackbox.Context

    def columnImpl[M[_], T](c: Context { type PrefixType = ColumnDataSource })(value: c.Expr[M[T]])
            (implicit ev1: c.WeakTypeTag[M[_]], ev2: c.WeakTypeTag[T]): c.Expr[c.prefix.value.Column[M, T]] = {
        import c.universe._

        val name = Macros.definingValName(c) getOrElse {
            c.abort(c.enclosingPosition, "column must be directly assigned to a val, such as `val x1 = column(List(1.0, 2.0, 3.0))`")
        }

        c.Expr[c.prefix.value.Column[M, T]](q"new Column(Symbol($name), $value)")
    }
}

@model abstract class RemoteSource extends DataSource {
    object data_url extends Field[String]
    object polling_interval extends Field[Int]
}

@model class AjaxDataSource extends RemoteSource {
    object method extends Field[HTTPMethod](HTTPMethod.POST)
}
