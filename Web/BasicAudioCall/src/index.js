import PanoRtc from '@pano.video/panortc'
import { find } from 'lodash-es';
let PanoDemo = {
  users: []
};

// UI
const input_rtcServer = document.getElementById('rtcServer');
const input_appId = document.getElementById('appID');
const input_token = document.getElementById('token');
const input_channel = document.getElementById('channelID');
const input_userId = document.getElementById('userID');

input_rtcServer.value = localStorage.getItem("PanoDemoRtcServer");
input_appId.value = localStorage.getItem("PanoDemoAppId");
input_token.value = localStorage.getItem("PanoDemoToken");
input_channel.value = localStorage.getItem("PanoDemoChannelId");
input_userId.value = localStorage.getItem("PanoDemoUserId");

const appId = input_appId.value;
const countdownDic = document.getElementById('countdown');

const button_joinChannel = document.getElementById('joinChannel');
const button_leaveChannel = document.getElementById('leaveChannel');
const textArea_roster = document.getElementById('meeting_roster');

const button_audio = document.getElementById('audioButton');
const button_mute_mic = document.getElementById('muteMic');
const button_get_mic = document.getElementById('getMics');
const button_get_speaker = document.getElementById('getSpeakers');

const select_mic = document.getElementById('mic_sel');
const select_speaker = document.getElementById('speaker_sel');

button_mute_mic.onclick = pano_muteMic;
button_get_mic.onclick = pano_getMics;
button_get_speaker.onclick = pano_getSpeakers;

window.PanoDemo = PanoDemo;


function init_UI () {
  input_userId.oninput = onInputUserId;
  button_mute_mic.disabled = true;
  button_audio.disabled = true;

  button_joinChannel.onclick = joinChannel;
  button_audio.onclick = startAudio;
  button_audio.innerHTML = 'Start Audio';
};

/*****************************************************************************************************************
 *                                         Global UI Functions                                                   *
 *****************************************************************************************************************
 */
function joinChannel() {
  button_joinChannel.innerText = 'Joining...';
  // init params
  PanoDemo.rtcServer = input_rtcServer.value;
  PanoDemo.appId = input_appId.value;
  PanoDemo.token = input_token.value;
  PanoDemo.channelId = input_channel.value;
  PanoDemo.userId = input_userId.value;
  PanoDemo.userName = document.getElementById('userName').value;

  localStorage.setItem('PanoDemoRtcServer', PanoDemo.rtcServer);
  localStorage.setItem('PanoDemoAppId', PanoDemo.appId);
  localStorage.setItem('PanoDemoToken', PanoDemo.token);
  localStorage.setItem('PanoDemoChannelId', PanoDemo.channelId);
  localStorage.setItem('PanoDemoUserId', PanoDemo.userId);

  document.querySelectorAll('input[name="channelMode"]').forEach((radio) => {
    if (radio.checked) {
      PanoDemo.channelMode =
        radio.value === 'meeting'
          ? PanoRtc.Constants.ChannelMode.TYPE_MEETING
          : PanoRtc.Constants.ChannelMode.TYPE_1_V_1
    }
  })
  if (
    !PanoDemo.appId ||
    !PanoDemo.channelId ||
    !PanoDemo.userId ||
    !PanoDemo.token
  ) {
    alert('请填写必要参数！')
    return
  }
  let rtcEngine = null;
  if(input_rtcServer.value){
    PanoRtc.RtcEngine.setServer(input_rtcServer.value);
  }
  rtcEngine = new PanoRtc.RtcEngine(appId);
  // For easily debug
  window.rtcEngine = rtcEngine;
  const eventTextarea = document.getElementById('events');
  rtcEngine.on = new Proxy(rtcEngine.on, {
    apply(target, object, args) {
      Reflect.apply(target, object, [args[0], params => {
        eventTextarea.value += new Date().toLocaleString();
        eventTextarea.value += ` ${JSON.stringify(params)}\r\n\r\n`;
        eventTextarea.scrollTop = eventTextarea.scrollHeight;
        Reflect.apply(args[1], object, [params]);
      }]);
    }
  });

  /*****************************************************************************************************************
   *                                         Events Handlers                                                       *
   *****************************************************************************************************************
  */

  rtcEngine.on(PanoRtc.RtcEngine.Events.joinChannelConfirm, data => {
    button_joinChannel.innerText = 'Join Channel';
    if (data.result !== 'success') {
      button_leaveChannel.disabled = true;
      button_leaveChannel.style.color = 'black';
      window.alert(`join channel failed because: ${data.message}`);
      return;
    }
    console.log('join channel success!');
    button_joinChannel.disabled = true;
    button_joinChannel.style.color = 'black';
    button_leaveChannel.disabled = false;
    button_leaveChannel.style.color = 'red';
    button_leaveChannel.onclick = () => leaveChannel();
    button_mute_mic.disabled = false;
    button_audio.disabled = false;
  });

  rtcEngine.on(PanoRtc.RtcEngine.Events.userListChange, result => {
    console.log('demo app: rosterChange', result);
    PanoDemo.users = result.users.map(user => {
      const oldUser = find(PanoDemo.users, { userId: user.userId }) || {};
      return Object.assign(oldUser, user);
    });
    updateRoster();
  });

  rtcEngine.on(PanoRtc.RtcEngine.Events.userLeave, (data) => {
    console.log('demo app: userleave,', data);
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.userJoin, (data) => {
    console.log('demo app: userjoin,', data);
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioMute, (data) =>
    console.log('demo app: userAudioMute,', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioUnmute, (data) =>
    console.log('demo app: userAudioUnmute,', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.firstAudioDataReceived, (data) =>
    console.log('demo app: firstAudioDataReceived', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.channelFailover, (data) =>
    console.error('demo app: channelFailover', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.audioDeviceChange, (data) =>
    console.log('demo app: audioDeviceChange', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioStart, (data) =>
    console.log('demo app: userAudioStart', data)
  );
  rtcEngine.on(PanoRtc.RtcEngine.Events.channelCountDown, (data) => {
    console.log('demo app: channelCountDown', data);
    PanoDemo.remainsec = data.remainsec;
    countdownDic.style.display = 'block';
    countdownDic.innerHTML = `remainsec: ${PanoDemo.remainsec}`;
    const interval = setInterval(() => {
      if (PanoDemo.remainsec > 0) {
        countdownDic.innerHTML = `remainsec: ${--PanoDemo.remainsec}`;
      } else {
        clearInterval(interval);
      }
    }, 1000);
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.leaveChannelIndication, (data) => {
    console.log('demo app: leaveChannelIndication', data);
    leaveChannel(true);
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.enumerateDeviceTimeout, (data) => {
    console.log('demo app: enumerateDeviceTimeout', data);
  });

  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioStart, (data) => {
    userMediaStatusUpdate(data, 'audio', 'unmute');
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioStop, (data) => {
    userMediaStatusUpdate(data, 'audio', 'closed');
  });
  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioMuted, (data) => {
    userMediaStatusUpdate(data, 'audio', 'mute');
  });

  rtcEngine.on(PanoRtc.RtcEngine.Events.userAudioUnmute, (data) => {
    userMediaStatusUpdate(data, 'audio', 'unmute');
  });

  let channelParam = {
    appId: PanoDemo.appId,
    token: PanoDemo.token,
    channelId: PanoDemo.channelId,
    channelMode: PanoDemo.channelMode,
    userId: PanoDemo.userId,
    userName: PanoDemo.userName,
    subscribeAudioAll: true
  };
  const joinChannelAPIResult = rtcEngine.joinChannel(channelParam, {
    joinChannelType: PanoRtc.Constants.JoinChannelType.mediaOnly
  });
  console.log('joinChannelAPIResult: ', joinChannelAPIResult);
} 

function leaveChannel(passive = false) {
  button_leaveChannel.disabled = true;
  button_leaveChannel.style.color = 'black';
  if (!passive) {
    rtcEngine.leaveChannel();
  }
  PanoDemo = {
    users: []
  };
  button_joinChannel.disabled = false;
  button_joinChannel.style.color = 'green';
  textArea_roster.value = '';
  countdownDic.style.display = 'none';
}

function updateRoster() {
  if (PanoDemo.users instanceof Array) {
    let list = '';
    PanoDemo.users.forEach(function (user) {
      list += 'Name: ' + user.userName + ', ID: ' + user.userId + ' \r\n' +
        'Audio: ' + (user.audioStatus ? user.audioStatus : 'closed') + ', Video: ' + (user.videoStatus ? user.videoStatus : 'closed') + ' \r\n \r\n';
    });
    textArea_roster.value = list;
  }
}

function userMediaStatusUpdate (data, kind, status) {
  console.log('demo app: receive user video status update,', data);
  if (PanoDemo.users instanceof Array) {
    for (let i = 0; i < PanoDemo.users.length; i++) {
      if (PanoDemo.users[i].userId === data.userId) {
        if (kind === 'audio') {
          PanoDemo.users[i].audioStatus = status;
        } else if (kind === 'video') {
          PanoDemo.users[i].videoStatus = status;
        }
        break;
      }
    }
    updateRoster();
  }
}


function pano_getMics() {
  rtcEngine.getMics(sucesss, fail);

  function sucesss(devices) {
    let length = select_mic.options.length;
    for (let i = length - 1; i >= 0; i--) {
      select_mic.options[i] = null;
    }

    for (let i = 0; i < devices.length; i++) {
      let option = document.createElement('option');
      option.text = devices[i].label;
      select_mic.add(option);
    }
  }
  function fail() {
    console.error('Failed to get mics.');
  }
}

function pano_getSpeakers() {
  rtcEngine.getSpeakers(sucesss, fail);

  function sucesss(devices) {
    let length = select_speaker.options.length;
    for (let i = length - 1; i >= 0; i--) {
      select_speaker.options[i] = null;
    }

    for (let i = 0; i < devices.length; i++) {
      let option = document.createElement('option');
      option.text = devices[i].label;
      select_speaker.add(option);
    }
  }

  function fail () {
    console.error('Failed to get speakers.');
  }
}

function pano_muteMic () {
  rtcEngine.muteMic();
  button_mute_mic.innerHTML = 'Unmute Mic';
  button_mute_mic.onclick = pano_unmuteMic;
}

function pano_unmuteMic () {
  rtcEngine.unmuteMic();
  button_mute_mic.innerHTML = 'Mute Mic';
  button_mute_mic.onclick = pano_muteMic;
}

function startAudio () {
  rtcEngine.startAudio();
  button_audio.innerHTML = 'Stop Audio';
  button_audio.onclick = stopAudio;
}

function stopAudio () {
  rtcEngine.stopAudio();
  button_audio.innerHTML = 'Start Audio';
  button_audio.onclick = startAudio;
}

function onInputUserId(event){
  setUserNameWith(event.target.value);
}

function setUserNameWith(userId){
  document.getElementById('userName').value = 'Web_' + userId;
}

(function () {
  if(input_userId.value){
    setUserNameWith(input_userId.value);
  }else{
    let rand = Math.random();
    let userId = '1908' + Math.round(rand * 9000);
    document.getElementById('userID').value = userId;
    setUserNameWith(userId);
  }
  init_UI();
})();
