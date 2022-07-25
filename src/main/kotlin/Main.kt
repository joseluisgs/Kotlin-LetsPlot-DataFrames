import jetbrains.datalore.base.values.Color
import jetbrains.letsPlot.Stat
import jetbrains.letsPlot.export.ggsave
import jetbrains.letsPlot.geom.geomBar
import jetbrains.letsPlot.geom.geomPoint
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.label.labs
import jetbrains.letsPlot.label.xlab
import jetbrains.letsPlot.label.ylab
import jetbrains.letsPlot.letsPlot
import models.accidentes.*
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.dataframe.io.writeJson
import utils.exportToHtml
import utils.exportToSvg
import utils.openInBrowser
import java.io.File

private val logger = KotlinLogging.logger {}

// https://kotlin.github.io/dataframe/overview.html
// https://lets-plot.org/index.html
// https://nbviewer.org/github/JetBrains/lets-plot-docs/blob/master/source/examples/cookbook/bar.ipynb


fun main() {
    println("Hello, Let's Plot!")

    // ejemploGrafica()
    ejemploAccidentes()

}

fun ejemploGrafica() {
    val xs = listOf(0, 0.5, 1, 2)
    val ys = listOf(0, 0.25, 1, 4)
    val data = mapOf<String, Any>("x" to xs, "y" to ys)

    val fig = letsPlot(data) + geomPoint(
        color = "dark-green",
        size = 4.0
    ) { x = "x"; y = "y" }

    // La salvamos
    ggsave(fig, "plot.png")

    // Exportamos en HTML
    openInBrowser(fig.exportToHtml(), "html-js.html")

    // Exportamos en SVG
    openInBrowser(fig.exportToSvg(), "html-svg.html")
}

private fun ejemploAccidentes() {
    println("Ejemplo de Accidentes")
    println("======================")
    // Vamos a leer los accidentes
    val accidentes by lazy { loadAccidentesFromCsv(File("data/accidentes.csv")) }

    // cargamos el DataFrame original
    val df by lazy { accidentes.toDataFrame() }

    //Mostramos el esquema e imprimimos las 5 primeras filas
    println("Esquema del DataFrame")
    println(df.schema())
    println(df.head(5))
    println("Numero de filas: ${df.rowsCount()}")
    df.select("numExpediente").print(10)

    val hombreVsMujeres = df.groupBy("sexo")
        .aggregate { count() into "total" }
        .filter { it.sexo == "Hombre" || it.sexo == "Mujer" }
    println("Numero de accidentes por sexo: ")
    println(hombreVsMujeres)

    var fig: Plot = letsPlot(data = hombreVsMujeres.toMap()) + geomBar(
        stat = Stat.identity,
        alpha = 0.8
    ) {
        x = "sexo"; y = "total"
    } + labs(
        x = "Sexo",
        y = "N. Accidentes",
        title = "Accidentes por Sexo",
    )
    openInBrowser(fig.exportToHtml(), "accidentesHombresVsMujeres.html")
    ggsave(fig, "porHombresVsMujeres.png")


    // Vamos a agrupar accidentes por mes
    val porMes = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
        }

    // Imprimimos el resultado
    println("Accidentes por mes:")
    println(porMes)

    fig = letsPlot(data = porMes.toMap()) + geomBar(
        stat = Stat.identity,
        alpha = 0.8
    ) {
        x = "fecha"; y = "numAccidentes"
    } + xlab("Mes") + ylab("Numero de accidentes") + ggtitle("Accidentes por mes")
    openInBrowser(fig.exportToHtml(), "accidentesPorMes.html")
    ggsave(fig, "porMes.png")

    // Media de accidentes por mes
    val mediaAccidentesPorMes = porMes.select("numAccidentes").mean()
    println("Media de accidentes por mes: $mediaAccidentesPorMes")

    // Estadisticas por mes hombres y mujeres
    val porMesSexo = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
            count { it.sexo == "Hombre" } into "numAccidentesHombres"
            count { it.sexo == "Mujer" } into "numAccidentesMujeres"
        }
    println("Accidentes por mes hombres y mujeres: ")
    println(porMesSexo)
    // Salvamos
    porMesSexo.writeCSV(File("reports/accidentesPorMesSexo.csv"))

    fig = letsPlot(data = porMesSexo.toMap()) + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.PACIFIC_BLUE,
    ) {
        x = "fecha"; y = "numAccidentes"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_GREEN
    ) {
        x = "fecha"; y = "numAccidentesHombres"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_MAGENTA,
    ) {
        x = "fecha"; y = "numAccidentesMujeres"
    } + labs(
        x = "Mes",
        y = "N. Accidentes",
        title = "Accidentes por Meses",
    )

    openInBrowser(fig.exportToHtml(), "accidentesPorMesSexo.html")
    ggsave(fig, "porMesSexo.png")

    // Estadisticas por distritos
    val porDistrito = df.groupBy { distrito }
        .aggregate {
            count() into "numAccidentes"
            count { it.sexo == "Hombre" } into "numAccidentesHombres"
            count { it.sexo == "Mujer" } into "numAccidentesMujeres"
        }.filter { it.distrito != "NULL" }
    println("Accidentes por distrito:")
    println(porDistrito)
    porDistrito.writeCSV(File("reports/accidentesPorDistritoSexo.csv"))

    // Pintamos
    fig = letsPlot(data = porDistrito.toMap()) + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.PACIFIC_BLUE,
    ) {
        x = "distrito"; y = "numAccidentes"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_GREEN
    ) {
        x = "distrito"; y = "numAccidentesHombres"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_MAGENTA,
    ) {
        x = "distrito"; y = "numAccidentesMujeres"
    } + labs(
        x = "Distrito",
        y = "N. Accidentes",
        title = "Accidentes por Distrito",
    )

    openInBrowser(fig.exportToHtml(), "accidentesPorDistritoSexo.html")
    ggsave(fig, "porDistritoSexo.png")

    // Estadisticas por meses con alcohol y drogas
    val porMesesAlcoholDrogas = df.convert { fecha }.with { it.month }
        .groupBy { fecha }
        .aggregate {
            count() into "numAccidentes"
            count { it.positivoAlcohol } into "numAccidentesAlcohol"
            count { it.positivoDrogas } into "numAccidentesDrogas"
            count { it.positivoAlcohol && it.positivoDrogas } into "numAccidentesAlcoholDrogas"
        }

    println("Accidentes por meses con alcohol y drogas:")
    println(porMesesAlcoholDrogas)
    porMesesAlcoholDrogas.writeJson(File("reports/accidentesPorMesesAlcoholDrogas.json"))

    fig = letsPlot(data = porMesesAlcoholDrogas.toMap()) /*+ geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.PACIFIC_BLUE,
    ) {
        x = "fecha"; y = "numAccidentes"
    }*/ + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_MAGENTA,
    ) {
        x = "fecha"; y = "numAccidentesAlcohol"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.ORANGE
    ) {
        x = "fecha"; y = "numAccidentesDrogas"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.RED
    ) {
        x = "fecha"; y = "numAccidentesAlcoholDrogas"
    } + labs(
        x = "Mes",
        y = "N. Accidentes",
        title = "Accidentes por Meses con Alcohol y Drogas",
    )
    openInBrowser(fig.exportToHtml(), "accidentesPorMesesAlcoholDrogas.html")
    ggsave(fig, "porMesesAlcoholDrogas.png")

    // Estadisticas alcohol y drogas po distrito
    val porDistritoAlcoholDrogas = df.groupBy { distrito }
        .aggregate {
            count() into "numAccidentes"
            count { it.positivoAlcohol } into "numAccidentesAlcohol"
            count { it.positivoDrogas } into "numAccidentesDrogas"
            count { it.positivoAlcohol && it.positivoDrogas } into "numAccidentesAlcoholDrogas"
        }.filter { it.distrito != "NULL" }.sortByDesc("numAccidentes")

    println("Accidentes por distrito con alcohol y drogas:")
    println(porDistritoAlcoholDrogas)
    porDistritoAlcoholDrogas.writeCSV(File("reports/accidentesPorDistritoAlcoholDrogas.csv"))

    fig = letsPlot(data = porDistritoAlcoholDrogas.toMap()) /*+ geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.PACIFIC_BLUE,
    ) {
        x = "distrito"; y = "numAccidentes"
    } */ + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.DARK_MAGENTA,
    ) {
        x = "distrito"; y = "numAccidentesAlcohol"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.ORANGE
    ) {
        x = "distrito"; y = "numAccidentesDrogas"
    } + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.RED
    ) {
        x = "distrito"; y = "numAccidentesAlcoholDrogas"
    } + labs(
        x = "Distrito",
        y = "N. Accidentes",
        title = "Accidentes por Distrito con Alcohol y Drogas",
    )
    openInBrowser(fig.exportToHtml(), "accidentesPorDistritoAlcoholDrogas.html")
    ggsave(fig, "porDistritoAlcoholDrogas.png")


    // Estadisticas por tipo de vehiculo
    val porTipoVehiculo = df.groupBy("tipoVehiculo")
        .aggregate {
            count() into "numAccidentes"
        }.filter { it.tipoVehiculo != "NULL" }
    println("Accidentes por tipo de vehiculo:")
    println(porTipoVehiculo)
    porTipoVehiculo.writeCSV(File("reports/accidentesPorTipoVehiculo.csv"))

    fig = letsPlot(data = porTipoVehiculo.toMap()) + geomBar(
        stat = Stat.identity,
        alpha = 0.8,
        fill = Color.PACIFIC_BLUE,
    ) {
        x = "tipoVehiculo"; y = "numAccidentes"
    } + labs(
        x = "Tipo de Vehiculo",
        y = "N. Accidentes",
        title = "Accidentes por Tipo de Vehiculo",
    )
    openInBrowser(fig.exportToHtml(), "accidentesPorTipoVehiculo.html")
    ggsave(fig, "porTipoVehiculo.png")

    // Estadisticas por distrito
    val porDistritoEstadisticas = porDistritoAlcoholDrogas
        .aggregate {
            sum("numAccidentes") into "numAccidentes"
            sum("numAccidentesAlcohol") into "numAccidentesAlcohol"
            sum("numAccidentesDrogas") into "numAccidentesDrogas"
            max("numAccidentes") into "numAccidentesMax"
            min("numAccidentes") into "numAccidentesMin"
            mean("numAccidentes") into "numAccidentesMedia"
            max("numAccidentesAlcohol") into "numAccidentesAlcoholMax"
            min("numAccidentesAlcohol") into "numAccidentesAlcoholMin"
            mean("numAccidentesAlcohol") into "numAccidentesAlcoholMedia"
            max("numAccidentesDrogas") into "numAccidentesDrogasMax"
            min("numAccidentesDrogas") into "numAccidentesDrogasMin"
            mean("numAccidentesDrogas") into "numAccidentesDrogasMedia"
            max("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMax"
            min("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMin"
            mean("numAccidentesAlcoholDrogas") into "numAccidentesAlcoholDrogasMedia"
        }
    println("Accidentes por distrito: ")
    println(porDistritoEstadisticas)
    porDistritoEstadisticas.writeJson(File("reports/accidentesPorDistritoEstadisticas.json"))

}