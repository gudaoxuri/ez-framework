package com.asto.ez.framework.rpc

object FileType {

  val TYPE_OFFICE = "office"
  val TYPE_TXT = "txt"
  val TYPE_COMPRESS = "compress"
  val TYPE_IMAGE = "image"
  val TYPE_AUDIO = "audio"
  val TYPE_VIDEO = "video"

  val types = Map(
    "office" -> List(
      FILE_TYPE.TXT.toString,
      FILE_TYPE.DOC.toString,
      FILE_TYPE.DOCX.toString,
      FILE_TYPE.XLS1.toString,
      FILE_TYPE.XLS2.toString,
      FILE_TYPE.XLSX.toString,
      FILE_TYPE.PPT1.toString,
      FILE_TYPE.PPT2.toString,
      FILE_TYPE.PPTX.toString
    ),
    "txt" -> List(
      FILE_TYPE.TXT.toString
    ),
    "compress" -> List(
      FILE_TYPE.TXT.toString,
      FILE_TYPE.ZIP1.toString,
      FILE_TYPE.ZIP2.toString,
      FILE_TYPE.GZIP.toString,
      FILE_TYPE.SEVENZ1.toString,
      FILE_TYPE.SEVENZ2.toString,
      FILE_TYPE.RAR1.toString,
      FILE_TYPE.RAR2.toString
    ),
    "image" -> List(
      FILE_TYPE.TXT.toString,
      FILE_TYPE.GIF.toString,
      FILE_TYPE.JPG1.toString,
      FILE_TYPE.JPG2.toString,
      FILE_TYPE.PNG.toString,
      FILE_TYPE.BMP1.toString,
      FILE_TYPE.BMP2.toString
    ),
    "audio" -> List(
      FILE_TYPE.TXT.toString,
      FILE_TYPE.MP3.toString,
      FILE_TYPE.WAV.toString,
      FILE_TYPE.WMA.toString
    ),
    "video" -> List(
      FILE_TYPE.TXT.toString,
      FILE_TYPE.MP4.toString,
      FILE_TYPE.MOV.toString,
      FILE_TYPE.MOVIE.toString,
      FILE_TYPE.WEBM.toString,
      FILE_TYPE.RM.toString,
      FILE_TYPE.RMVB.toString,
      FILE_TYPE.AVI.toString
    )
  )

}

object FILE_TYPE extends Enumeration {
  type FILE_TYPE = Value
  val HTML = Value("text/html")
  val XML = Value("text/xml")
  val TXT = Value("text/plain")
  val JS = Value("application/x-javascript")
  val CSS = Value("text/css")

  val DOC = Value("application/msword")
  val DOCX = Value("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
  val XLS1 = Value("application/x-xls")
  val XLS2 = Value("application/vnd.ms-excel")
  val XLSX = Value("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  val PPT1 = Value("application/x-ppt")
  val PPT2 = Value("application/vnd.ms-powerpoint")
  val PPTX = Value("application/vnd.openxmlformats-officedocument.presentationml.presentation")

  val ZIP1 = Value("application/zip")
  val ZIP2 = Value("application/x-zip-compressed")
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
  val WAV = Value("audio/wav")
  val WMA = Value("audio/x-ms-wma")

  val MP4 = Value("video/mpeg4")
  val MOV = Value("video/quicktime")
  val AVI = Value("video/avi")
  val MOVIE = Value("video/x-sgi-movie")
  val WEBM = Value("audio/webm")
  val RM = Value("audio/x-pn-realaudio")
  val RMVB = Value("application/vnd.rn-realmedia-vbr")

}
