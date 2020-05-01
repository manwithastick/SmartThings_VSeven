/**
 *  Copyright 2019 RGBgenie
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *  Z-wave RGBW Controller (White chanel is dimmed and doesn't support color temperature)
 *
 *  Author: vseven (Allan) / RGBgenie
 *  Date: 2019-02-13
 */

metadata {
	definition (name: "RGBgenie ZW 1002 Channels", namespace: "vseven", author: "RGBgenie", ocfDeviceType: "oic.d.light", mnmn: "SmartThings", vid: "generic-rgbw-color-bulb") {
		capability "Switch Level"
		capability "Switch"
		capability "Refresh"
		capability "Actuator"
		capability "Sensor"
		capability "Health Check"
		capability "Light"

		command "setChannel1Level"
		command "setChannel2Level"
		command "setChannel3Level"
		command "setChannel4Level"
    
		attribute "channel1Level", "number"
		attribute "channel2Level", "number"
		attribute "channel3Level", "number"
		attribute "channel4Level", "number"

		fingerprint mfr: "0330 ", prod: "0201", model: "D002", deviceJoinName: "RGBgenie RGBW Controller ZW-1002"
	}

	simulator {
	}

	tiles(scale: 2) {
		multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState("on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
				attributeState("turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00a0dc", nextState:"turningOff")
				attributeState("turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn")
			}
			tileAttribute ("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action:"switch level.setLevel"
			}
		}
  }
  controlTile("channel1SliderControl", "device.channel1Level", "slider", width: 3, height: 2, inactiveLabel: false) {
    state "channel1Level", action:"setChannel1Level", label:'Channel 1'
 	 }
  controlTile("channel2SliderControl", "device.channel2Level", "slider", width: 3, height: 2, inactiveLabel: true) {
    state "channel2Level", action:"setChannel2Level", label:'Channel 2'
 	 }
  controlTile("channel3SliderControl", "device.channel3Level", "slider", width: 3, height: 2, inactiveLabel: false) {
    state "channel3Level", action:"setChannel3Level", label:'Channel 3'
 	 }
  controlTile("channel4SliderControl", "device.channel4Level", "slider", width: 3, height: 2, inactiveLabel: false) {
    state "channel4Level", action:"setChannel4Level", label:'Channel 4'
 	 }
	standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
		state "default", label:"Refresh", action:"refresh.refresh", icon:"st.secondary.refresh"
	}

	main(["switch"])
	details(["switch", "channel1SliderControl", "channel2SliderControl", "channel3SliderControl", "channel4SliderControl", "refresh"])
}

def updated() {
     // Device-Watch simply pings if no device events received for 122 min (checkInterval)
     log.debug("Adding checkInterval for health checks")
     sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
     response(refresh())
}

def installed() {
     // Device-Watch simply pings if no device events received for 122 min (checkInterval)
     log.debug("Adding checkInterval for health checks")
     sendEvent(name: "checkInterval", value: 2 * 60 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
}

def parse(description) {
	def result = null
	if (description != "updated") {
		def cmd = zwave.parse(description)
		if (cmd) {
			result = zwaveEvent(cmd)
			log.debug("'$description' parsed to $result")
		} else {
			log.debug("Couldn't zwave.parse '$description'")
		}
	}
	result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	def result = [createEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")]
	if (cmd.value) {
		result << createEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	response(command(zwave.switchMultilevelV1.switchMultilevelGet()))
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
  def encapsulatedCommand = cmd.encapsulatedCommand()
  if (encapsulatedCommand) {
    zwaveEvent(encapsulatedCommand)
  }
}


def zwaveEvent(physicalgraph.zwave.Command cmd) {
	def linkText = device.label ?: device.name
	[linkText: linkText, descriptionText: "$linkText: $cmd", displayed: false]
}

def on() {
	command(zwave.basicV1.basicSet(value: 0xFF))
}

def off() {
	command(zwave.basicV1.basicSet(value: 0x00))
}

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {
	if(level > 99) level = 99
	command(zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration))
}

def refresh() {
	commands([
		zwave.switchMultilevelV1.switchMultilevelGet(),
	], 1000)
}

def setChannel1Level(percent) {
	def result = []
	if(percent > 99) percent = 100
	sendEvent(name: "channel1Level", value: percent)
	def full = percent * 2.55
	result << zwave.switchColorV3.switchColorSet(red: full)
    
	commands(result)
}

def setChannel2Level(percent) {
	def result = []
	if(percent > 99) percent = 99
	sendEvent(name: "channel2Level", value: percent)
	def full = percent * 2.55
	result << zwave.switchColorV3.switchColorSet(green: full)
    
	commands(result)
}

def setChannel3Level(percent) {
	def result = []
	if(percent > 99) percent = 99
	sendEvent(name: "channel3Level", value: percent)
	def full = percent * 2.55
	result << zwave.switchColorV3.switchColorSet(blue: full)
    
	commands(result)
}

def setChannel4Level(percent) {
	def result = []
	if(percent > 99) percent = 99
	sendEvent(name: "channel4Level", value: percent)
	def full = percent * 2.55
	result << zwave.switchColorV3.switchColorSet(warmWhite: full)
    
	commands(result)
}

private command(physicalgraph.zwave.Command cmd) {
  if (zwaveInfo.zw.contains("s")) {
	  zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
	} else {
		cmd.format()
	}
}

private commands(commands, delay=400) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def ping() {
	refresh()
}
