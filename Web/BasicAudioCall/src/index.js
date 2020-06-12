import PanoRtc from '@pano.video/panortc'

const panoDemo = {
  users: []
}

let remainsecInterval = ''
const appId = document.getElementById('appID').value
const countdownDic = document.getElementById('countdown')
const rtcEngine = new PanoRtc.RtcEngine(appId || '-')
window.rtcEngine = rtcEngine

rtcEngine.on(PanoRtc.RtcEngine.Events.joinChannelConfirm, function (data) {
  console.log('demo app: joinChannelConfirm', data)
  leaveChannelBtn.disabled = false
  leaveChannelBtn.style.color = 'red'
  leaveChannelBtn.onclick = () => leaveChannel()
})

rtcEngine.on(PanoRtc.RtcEngine.Events.usersListChange, function (result) {
  console.log('demo app: rosterChange', result)
  panoDemo.users = result.users
  if (panoDemo.users instanceof Array) {
    let list = ''
    panoDemo.users.forEach(function (user) {
      list +=
        user.userName +
        ', id: ' +
        user.userId +
        ', role: ' +
        user.role +
        ' \r\n'
    })
    userListTextArea.value = list
  }
})

rtcEngine.on(PanoRtc.RtcEngine.Events.userLeave, (data) => {
  console.log('demo app: userleave,', data)
})
rtcEngine.on(PanoRtc.RtcEngine.Events.userJoin, (data) => {
  console.log('demo app: userleave,', data)
})
rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioMute, (data) =>
  console.log('demo app: userAudioMute,', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioUnMute, (data) =>
  console.log('demo app: userAudioUnMute,', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.whiteboardAvailable, (data) =>
  console.log('demo app: whiteboardAvailable', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.whiteboardUnavailable, (data) =>
  console.log('demo app: whiteboardUnavailable', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.whiteboardStart, (data) =>
  console.log('demo app: whiteboardStart', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.whiteboardStop, (data) =>
  console.log('demo app: whiteboardStop', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.firstAudioDataReceived, (data) =>
  console.log('demo app: firstAudioDataReceived', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.channelFailover, (data) =>
  console.error('demo app: channelFailover', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.audioDeviceChange, (data) =>
  console.log('demo app: audioDeviceChange', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.videoDeviceChange, (data) =>
  console.log('demo app: videoDeviceChange', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.userExpelled, (data) =>
  console.log('demo app: userExpelled', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioStart, (data) =>
  console.log('demo app: userAudioStart', data)
)
rtcEngine.on(PanoRtc.RtcEngine.Events.channelCountDown, (data) => {
  console.log('demo app: channelCountDown', data)
  panoDemo.remainsec = data.remainsec
  countdownDic.style.display = 'block'
  countdownDic.innerHTML = `remainsec: ${panoDemo.remainsec}`
  remainsecInterval = setInterval(() => {
    if (panoDemo.remainsec > 0) {
      countdownDic.innerHTML = `remainsec: ${--panoDemo.remainsec}`
    } else {
      clearInterval(remainsecInterval)
      leaveChannel()
    }
  }, 1000)
})
rtcEngine.on(PanoRtc.RtcEngine.Events.leaveChannelIndication, (data) => {
  console.log('demo app: leaveChannelIndication', data)
  leaveChannel(true)
})
// rtcEngine.on(PanoRtc.RtcEngine.Events.activeSpeakerListUpdate, data => console.log('demo app: activeSpeakerListUpdate', data))

const joinChannelBtn = document.getElementById('joinChannel')
const leaveChannelBtn = document.getElementById('leaveChannel')
const userListTextArea = document.getElementById('meeting_roster')

joinChannelBtn.onclick = () => {
  // init params
  panoDemo.appId = document.getElementById('appID').value
  panoDemo.channelId = document.getElementById('channelID').value
  panoDemo.userId = document.getElementById('userID').value
  panoDemo.userName = document.getElementById('userName').value
  panoDemo.token = document.getElementById('token').value
  document.querySelectorAll('input[name="channelMode"]').forEach((radio) => {
    if (radio.checked) {
      panoDemo.channelMode =
        radio.value === 'meeting'
          ? PanoRtc.Constants.ChannelMode.TYPE_MEETING
          : PanoRtc.Constants.ChannelMode.TYPE_1_V_1
    }
  })
  if (
    !panoDemo.appId ||
    !panoDemo.channelId ||
    !panoDemo.userId ||
    !panoDemo.token
  ) {
    alert('请填写必要参数！')
    return
  }
  joinChannelBtn.disabled = true
  joinChannelBtn.style.color = 'black'

  // 使用创建的临时 token
  const channelParam = {
    appId: panoDemo.appId,
    token: panoDemo.token,
    channelId: panoDemo.channelId,
    channelMode: panoDemo.channelMode,
    userId: panoDemo.userId,
    userName: panoDemo.userName
  }
  rtcEngine.joinChannel(channelParam, {
    joinChannelType: PanoRtc.Constants.JoinChannelType.mediaOnly
  })
}

function leaveChannel(passive = false) {
  leaveChannelBtn.disabled = true
  leaveChannelBtn.style.color = 'black'
  clearInterval(remainsecInterval)
  if (!passive) {
    rtcEngine.leaveChannel()
  }
  joinChannelBtn.disabled = false
  joinChannelBtn.style.color = 'green'
  userListTextArea.value = ''

  setTimeout(function () {
    // location.reload(true);
  }, 2000)
}

// Test features
const muteMicBtn = document.getElementById('muteMic')
const getMicBtn = document.getElementById('getMics')
const getSpeakerBtn = document.getElementById('getSpeakers')

const micSelector = document.getElementById('mic_sel')
const speakerSelector = document.getElementById('speaker_sel')

speakerSelector.addEventListener('change', (e) => {
  rtcEngine.selectSpeaker(e.target.value)
})

micSelector.addEventListener('change', (e) => {
  rtcEngine.selectMic(e.target.value)
})

muteMicBtn.onclick = muteMic
getMicBtn.onclick = getMics
getSpeakerBtn.onclick = getSpeakers

function getMics() {
  rtcEngine.getMics(sucesss, fail)

  function sucesss(devices) {
    const length = micSelector.options.length
    for (let i = length - 1; i >= 0; i--) {
      micSelector.options[i] = null
    }

    for (let i = 0; i < devices.length; i++) {
      const option = document.createElement('option')
      option.text = devices[i].label
      option.value = devices[i].deviceId
      micSelector.add(option)
    }
  }
  function fail() {
    console.error('Failed to get mics.')
  }
}

function getSpeakers() {
  rtcEngine.getSpeakers(sucesss, fail)

  function sucesss(devices) {
    const length = speakerSelector.options.length
    for (let i = length - 1; i >= 0; i--) {
      speakerSelector.options[i] = null
    }

    for (let i = 0; i < devices.length; i++) {
      const option = document.createElement('option')
      option.text = devices[i].label
      option.value = devices[i].deviceId
      speakerSelector.add(option)
    }
  }

  function fail() {
    console.error('Failed to get speakers.')
  }
}
function muteMic() {
  rtcEngine.muteMic()
  muteMicBtn.innerHTML = 'Unmute Mic'
  muteMicBtn.onclick = unmuteMic
}

function unmuteMic() {
  rtcEngine.unmuteMic()
  muteMicBtn.innerHTML = 'Mute Mic'
  muteMicBtn.onclick = muteMic
}

;(function () {
  const userId = '1908' + Math.round(Math.random() * 9000)
  document.getElementById('userID').value = userId
  document.getElementById('userName').value = 'Panortc-' + userId
})()
