package com.ecfront.ez.framework.core.helper

object FileType {

  private val types = Map(
    "office" -> List(
      EFileType.TXT.toString,
      EFileType.DOC.toString,
      EFileType.DOCX.toString,
      EFileType.XLS1.toString,
      EFileType.XLS2.toString,
      EFileType.XLSX.toString,
      EFileType.PPT1.toString,
      EFileType.PPT2.toString,
      EFileType.PPTX.toString,
      EFileType.PDF.toString
    ),
    "txt" -> List(
      EFileType.TXT.toString
    ),
    "compress" -> List(
      EFileType.ZIP1.toString,
      EFileType.ZIP2.toString,
      EFileType.ZIP3.toString,
      EFileType.GZIP.toString,
      EFileType.SEVENZ1.toString,
      EFileType.SEVENZ2.toString,
      EFileType.RAR1.toString,
      EFileType.RAR2.toString
    ),
    "image" -> List(
      EFileType.GIF.toString,
      EFileType.JPG1.toString,
      EFileType.JPG2.toString,
      EFileType.PNG.toString,
      EFileType.BMP1.toString,
      EFileType.BMP2.toString
    ),
    "audio" -> List(
      EFileType.MP3.toString,
      EFileType.WAV1.toString,
      EFileType.WAV2.toString,
      EFileType.WMA.toString
    ),
    "video" -> List(
      EFileType.MP4.toString,
      EFileType.MOV.toString,
      EFileType.MOVIE.toString,
      EFileType.WEBM.toString,
      EFileType.RM.toString,
      EFileType.RMVB.toString,
      EFileType.AVI1.toString,
      EFileType.AVI2.toString,
      EFileType.AVI3.toString
    )
  )

  val TYPE_OFFICE = types("office")
  val TYPE_TXT = types("txt")
  val TYPE_COMPRESS = types("compress")
  val TYPE_IMAGE = types("image")
  val TYPE_AUDIO = types("audio")
  val TYPE_VIDEO = types("video")

}

object EFileType extends Enumeration {
  type FILE_TYPE = Value
  val HTML1 = Value("text/html")
  val HTML2 = Value("application/html")
  val XML1 = Value("text/xml")
  val XML2 = Value("application/xml")
  val TXT = Value("text/plain")
  val JS1 = Value("application/javascript")
  val JS2 = Value("application/x-javascript")
  val CSS = Value("text/css")

  val DOC = Value("application/msword")
  val DOCX = Value("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
  val XLS1 = Value("application/x-xls")
  val XLS2 = Value("application/vnd.ms-excel")
  val XLSX = Value("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  val PPT1 = Value("application/x-ppt")
  val PPT2 = Value("application/vnd.ms-powerpoint")
  val PPTX = Value("application/vnd.openxmlformats-officedocument.presentationml.presentation")
  val PDF = Value("application/pdf")

  val ZIP1 = Value("application/zip")
  val ZIP2 = Value("application/x-zip-compressed")
  val ZIP3 = Value("application/x-compressed-zip")
  val GZIP = Value("application/gzip")
  val SEVENZ1 = Value("application/x-7z-compressed")
  val SEVENZ2 = Value("application/octet-stream")
  val RAR1 = Value("application/rar")
  val RAR2 = Value("application/x-rar-compressed")

  val GIF = Value("image/gif")
  val JPG1 = Value("image/jpeg")
  val JPG2 = Value("image/pjpeg")
  val PNG = Value("image/png")
  val BMP1 = Value("application/x-bmp")
  val BMP2 = Value("image/bmp")

  val MP3 = Value("audio/mp3")
  val WAV1 = Value("audio/wav")
  val WAV2 = Value("audio/x-wav")
  val WMA = Value("audio/x-ms-wma")

  val MP4 = Value("video/mpeg4")
  val MOV = Value("video/quicktime")
  val AVI1 = Value("video/avi")
  val AVI2 = Value("video/x-msvideo")
  val AVI3 = Value("video/msvideo")
  val MOVIE = Value("video/x-sgi-movie")
  val WEBM = Value("audio/webm")
  val RM = Value("audio/x-pn-realaudio")
  val RMVB = Value("application/vnd.rn-realmedia-vbr")

}
