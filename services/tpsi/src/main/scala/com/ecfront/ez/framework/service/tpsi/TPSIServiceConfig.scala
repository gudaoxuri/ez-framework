package com.ecfront.ez.framework.service.tpsi

import com.fasterxml.jackson.databind.JsonNode


case class TPSIServiceConfig(
                            code: String,
                            ratePerMinute: Int,
                            expireMinutes: Int,
                            needLogin: Boolean,
                            isMock: Boolean,
                            servicePath: String,
                            args: JsonNode)
