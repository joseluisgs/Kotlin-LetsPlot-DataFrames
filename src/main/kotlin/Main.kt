import jetbrains.datalore.base.values.Color
import jetbrains.letsPlot.*
import jetbrains.letsPlot.export.ggsave
import jetbrains.letsPlot.geom.*
import jetbrains.letsPlot.intern.Plot
import jetbrains.letsPlot.label.ggtitle
import jetbrains.letsPlot.label.labs
import jetbrains.letsPlot.label.xlab
import jetbrains.letsPlot.label.ylab
import jetbrains.letsPlot.sampling.samplingPick
import jetbrains.letsPlot.scale.scaleColorGradient
import jetbrains.letsPlot.scale.scaleFillGradient
import jetbrains.letsPlot.scale.scaleFillHue
import jetbrains.letsPlot.scale.scaleFillManual
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.yearsUntil
import models.accidentes.*
import models.netflix.*
import mu.KotlinLogging
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.*
import org.jetbrains.kotlinx.dataframe.io.readCSV
import org.jetbrains.kotlinx.dataframe.io.writeCSV
import org.jetbrains.kotlinx.dataframe.io.writeJson
import org.jetbrains.kotlinx.dataframe.size
import utils.exportToHtml
import utils.exportToSvg
import utils.openInBrowser
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

private val logger = KotlinLogging.logger {}

// https://kotlin.github.io/dataframe/overview.html
// https://lets-plot.org/index.html
// https://nbviewer.org/github/JetBrains/lets-plot-docs/blob/master/source/examples/cookbook/bar.ipynb


fun main() {
    println("Hello, Let's Plot!")

    // ejemploGrafica()
    // ejemploAccidentes()
    ejemploNetflix()
}

fun ejemploNetflix() {
    // Para manejar las fechas!
    Locale.setDefault(Locale.US)

    val rawdf = DataFrame.readCSV("data/netflix_titles.csv")
    val origin = rawdf.cast<NetflixItem>()
    // imprimimos el esquema
    origin.schema().print()
    println(origin.size()) // rowsCount x columnsCount
    origin.head().print() // return las primeras 5 filas
    // Estadisticas generales e info de cada columna
    origin.describe().print()

    val df =
        origin.dropNulls { date_added } // eliminamos donde no hay fecha y transformamos a LocalDate de Kotlin que luego para operar vendrá mejor
            .convert { it["date_added"] }  // si lo cojo así cojo el string que almacena y no su objecto casteado, como arriba
            .with {
                LocalDate.parse(it.toString().trim(), DateTimeFormatter.ofPattern("MMMM d, yyyy"))
            }

    df.head().print()

    // Mejor usar esta sintaxis de ggp
    var fig = ggplot(origin.toMap()) +
            geomBar { x = "type"; fill = "type" } +
            scaleFillManual(listOf("#00BCD4", "#009688")) +
            ggsize(900, 550) +
            labs(
                x = "Tipo",
                y = "Total",
                title = "Número de películas y series en Netflix",
            )

    ggsave(fig, "netflix01.png")
    openInBrowser(fig.exportToHtml(), "netflix01.html")

    // Vamos a trabajar solo con el año de las fechas
    var df_dateCount = df.convert { date_added }.with { it!!.year } //solo el año
        .groupBy { date_added } // agrupamos por año
        .aggregate {
            count { type == "TV Show" } into "tvshows" // contamos los tvshows
            count { type == "Movie" } into "movies" // contamos los movies
        }
    df_dateCount.head().print()

    // Podemos simplificarlo más
    df_dateCount = df.groupBy { date_added.map { it!!.year } } // Mapeamos por el año
        .aggregate {
            count { type == "TV Show" } into "tvshows"
            count { type == "Movie" } into "movies"
        }

    df_dateCount.head().print()

    // Vamos a pivotar
    df.groupBy { date_added.map { it!!.year } }
        .pivot { type }

    df.groupBy { date_added.map { it!!.year } }
        .pivot { type }.aggregate { count() }

    // Finalmente se queda en:
    df.groupBy { date_added.map { it!!.year } }
        .pivot { type }.count()

    // Podemos hacer uso de API pivotCounts()
    df_dateCount = df.groupBy { date_added.map { it!!.year } }.pivotCounts { type }
    df_dateCount.head().print()

    // Ahora usamos flatten para que sea una tabla de 2 columnas
    val df_dateCountFlatten = df_dateCount.flatten()
    df_dateCountFlatten.head().print()

    // Pintamos
    fig = ggplot(df_dateCountFlatten.toMap()) +
            geomArea(color = "#BF360C", fill = "#BF360C", alpha = 0.5) { x = "date_added"; y = "TV Show" } +
            geomArea(color = "#01579B", fill = "#01579B", alpha = 0.5) { x = "date_added"; y = "Movie" } +
            theme(
                panelBackground = elementRect(color = "#ECEFF1", fill = "#ECEFF1"),
                panelGrid = elementBlank(),
            ) +
            labs(
                x = "Año",
                y = "Total",
                title = "Número de películas y series en Netflix",
            ) + ggsize(800, 500)

    ggsave(fig, "netflix02.png")
    openInBrowser(fig.exportToHtml(), "netflix02.html")

    // Suma acumulativa de las peliculas sobre los shows
    val df_cumSumTitles = df_dateCount
        .sortBy { date_added } // ordenamos por fecha
        .cumSum { type.all() } // cuente la suma acumulada para las columnas 'Programa de TV' y 'Película' que están anidadas en la columna 'tipo'
    df_cumSumTitles.head().print()

    fig = ggplot(df_cumSumTitles.flatten().toMap()) +
            geomArea(color = "#BF360C", fill = "#BF360C", alpha = 0.5) { x = "date_added"; y = "TV Show" } +
            geomArea(color = "#01579B", fill = "#01579B", alpha = 0.5) { x = "date_added"; y = "Movie" } +
            theme(
                panelBackground = elementRect(color = "#ECEFF1", fill = "#ECEFF1"),
                panelGrid = elementBlank(),
            ) +
            labs(
                x = "Año",
                y = "Conteo acumulativo",
                title = "Recuento acumulado de títulos por año",
            ) + ggsize(800, 500)
    ggsave(fig, "netflix03.png")
    openInBrowser(fig.exportToHtml(), "netflix03.html")

    // Vamos a analizar el tiempo de Kotlin que para esto es muy chulo!!!
    val maxDate = df.date_added.max().toKotlinLocalDate()
    val dfDays = df.add {
        "days_on_platform" from {
            date_added!!.toKotlinLocalDate().daysUntil(maxDate)
        } // Numero de dias en la plataforma
        "months_on_platform" from {
            date_added!!.toKotlinLocalDate().monthsUntil(maxDate)
        } // numero de meses en la plataforma
        "years_on_platform" from {
            date_added!!.toKotlinLocalDate().yearsUntil(maxDate)
        } // // numero de años en la plataforma
    }

    fig = ggplot(dfDays.select("type", "years_on_platform").toMap()) + // las dos columnas
            geomBar(position = Pos.dodge) { x = "years_on_platform"; fill = "type" } +
            scaleFillManual(listOf("#bc3076", "#30bc76")) +
            xlab("years") +
            ggtitle("Años en emisión de películas y programas de televisión en Netflix") +
            ggsize(900, 500)
    ggsave(fig, "netflix04.png")
    openInBrowser(fig.exportToHtml(), "netflix04.html")

    // añadimos la columna diferencia entre año de lanzamienrto  y año de emisión
    val dfYears = df.add("years_off_platform") {
        date_added!!.year - release_year
    }.filter { "years_off_platform"<Int>() > 0 } // quitamos lo que sea menor o igual a cero
    dfYears.head().print()

    fig = ggplot(dfYears.select("years_off_platform").toMap()) +
            geomPoint(stat = Stat.count(), size = 7.5) { x = "years_off_platform"; color = "years_off_platform" } +
            scaleColorGradient(low = "#97a6d0", high = "#00256e") +
            theme().legendPosition(0.9, 0.83) +
            xlab("años") +
            ggtitle("¿Cuánto tiempo se tarda en agregar un título a Netflix?") +
            ggsize(1000, 500)
    ggsave(fig, "netflix05.png")
    openInBrowser(fig.exportToHtml(), "netflix05.html")

    // Las 5 peliculas más viejas
    dfDays
        .filter { type == "Movie" } // Por películas
        .sortByDesc("days_on_platform") // Ordenadas por días
        .select { type and title and country and date_added and release_year and duration } // Delecionamos as columnas
        // tamnbien puedes as´i hacer:.select("type", "title", "country", "date_added", "release_year", "duration")
        .head() //obtenemos los primeros 5

    // Las películas más recientes
    dfDays
        .filter { type == "Movie" }
        .sortBy("days_on_platform")
        .select { type and title and country and date_added and release_year and duration }
        .head()

    // Los shows mas viejos
    dfDays
        .filter { type == "TV Show" }
        .sortByDesc("days_on_platform")
        .select { type and title and country and date_added and release_year and duration }
        .head()

    // LLos shows mas recientes
    dfDays
        .filter { type == "TV Show" }
        .sortBy("days_on_platform")
        .select { type and title and country and date_added and release_year and duration }
        .head()

    // dividir fechas en cuatro columnas, los meses en que se agregan menos peliculas y shows
    val dfSpliDate = df
        .split { date_added }.by { listOf(it, it.dayOfWeek, it.month, it.year) }
        .into("date", "day", "month", "year")
        .sortBy("month") // sorting by month

    fig = ggplot(
        dfSpliDate
            .groupBy("year", "month").count() // contamos cuantos titulos se han añadido cada año y mes
            .convert("month").toStr() // convertimos el mes en cadena para pintarlo
            .toMap()
    ) +
            geomTile(height = 0.9, width = 0.9) { x = "year"; y = "month"; fill = "count" } +
            theme(panelBackground = elementBlank(), panelGrid = elementBlank()) +
            scaleFillGradient(low = "#FFF3E0", high = "#E65100") +
            ggtitle("Contenido nuevo por mes y año") +
            ggsize(900, 700)

    ggsave(fig, "netflix06.png")
    openInBrowser(fig.exportToHtml(), "netflix06.html")

    // Vamos a ver los directores que más aparecen en Netflix
    val dfDirectors = df.valueCounts { director }
    fig = ggplot(dfDirectors.toMap()) +
            geomHistogram(
                stat = Stat.identity, sampling = samplingPick(10), color = "#E8F5E9", boundary = 1.0, showLegend = false
            ) { x = "director"; y = "count"; fill = "director" } +
            scaleFillHue() +
            coordFlip() +
            xlab("Nambre") +
            ggtitle("Top 10 Directores") +
            ggsize(850, 500)

    ggsave(fig, "netflix07.png")
    openInBrowser(fig.exportToHtml(), "netflix07.html")


    // Puntuaciones, top 5
    val dfRating = df.groupBy("rating").count().sortByDesc("count")
    dfRating.head().print()
    fig = ggplot(origin.toMap()) +
            geomBar(position = Pos.dodge) { x = "rating"; fill = "type" } +
            scaleFillManual(listOf("#00BCD4", "#009688")) +
            ggsize(900, 550) +
            labs(
                x = "Puntuación",
                y = "Total",
                title = "Calificación de películas y series en Netflix",
            )

    ggsave(fig, "netflix08.png")
    openInBrowser(fig.exportToHtml(), "netflix08.html")

    fig = ggplot(origin.toMap()) +
            geomHistogram(boundary = 1.0, color = 0xE0F7FA, showLegend = false) { x = "rating"; fill = "rating" } +
            scaleFillHue() +
            xlab("Rating") +
            ggtitle("Calificación de títulos") +
            ggsize(950, 500)

    ggsave(fig, "netflix09.png")
    openInBrowser(fig.exportToHtml(), "netflix09.html")

    val dfDur = df
        .split { duration }.by(" ")
        .inward("duration_num", "duration_scale") // dividir la duración por tiempo y escalar hacia adentro
        .convert { "duration"["duration_num"] }.toInt() // conversión por ruta de columna
        .update { "duration"["duration_scale"] }.with { if (it == "Seasons") "Season" else it }
    dfDur.head().print()

    // Paises
    val dfPaises = DataFrame.readCSV("data/country_codes.csv")
    dfPaises.head().print()

    // contyando el número de paises y haciendo join con la tabla de paises
    val dfPeliculasPaises = df.valueCounts { country }.join(dfPaises)
    dfPeliculasPaises.head().print()

    fig = ggplot(dfPeliculasPaises[0..9].sortByDesc("count").toMap()) +
            geomBar(stat = Stat.identity, fill = "#00796B") { x = "country"; y = "count" } +
            ggtitle("Top 10 Countries") +
            ggsize(900, 450)

    ggsave(fig, "netflix10.png")
    openInBrowser(fig.exportToHtml(), "netflix10.html")

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
    df.cast<Accidente>() // Lo casteamos a Accidente

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