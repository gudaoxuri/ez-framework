package com.ecfront.ez.framework.service.message.helper


import org.apache.ignite.configuration.IgniteConfiguration
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder
import org.apache.ignite.{Ignite, Ignition}

import scala.collection.JavaConversions._

object IgniteHelper {

  var ignite: Ignite = _

  def init(addresses: List[String], multicastGroup: String): Unit = {
    val cfg = new IgniteConfiguration()
    if (addresses.nonEmpty || multicastGroup.nonEmpty) {
      val spi = new TcpDiscoverySpi()
      if (addresses.nonEmpty && multicastGroup.nonEmpty) {
        val ipFinder = new TcpDiscoveryMulticastIpFinder()
        ipFinder.setAddresses(addresses)
        ipFinder.setMulticastGroup(multicastGroup)
        spi.setIpFinder(ipFinder)
      } else if (addresses.nonEmpty) {
        val ipFinder = new TcpDiscoveryVmIpFinder()
        ipFinder.setAddresses(addresses)
        spi.setIpFinder(ipFinder)
      } else if (multicastGroup.nonEmpty) {
        val ipFinder = new TcpDiscoveryMulticastIpFinder()
        ipFinder.setMulticastGroup(multicastGroup)
        spi.setIpFinder(ipFinder)
      }
      cfg.setDiscoverySpi(spi)
    }
    ignite = Ignition.start(cfg)
  }

}