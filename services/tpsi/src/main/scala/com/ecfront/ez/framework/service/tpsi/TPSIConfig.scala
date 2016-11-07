package com.ecfront.ez.framework.service.tpsi

import com.fasterxml.jackson.databind.JsonNode


case class TPSIConfig(
                            var code: String,
                            ratePerMinute: Int,
                            expireMinutes: Int,
                            needLogin: Boolean,
                            isMock: Boolean,
                            isStorage: Boolean,
                            args: JsonNode)
