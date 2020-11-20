//
//  ChannelViewController.m
//  BasicAudioCall
//
//  Copyright © 2020 Pano. All rights reserved.
//

#import "ChannelViewController.h"
#import "ChannelInfo.h"

@interface ChannelViewController () <PanoRtcEngineDelegate>

@property (strong, nonatomic) IBOutlet UILabel * message;
@property (strong, nonatomic) PanoRtcEngineKit * engineKit;

@end

@implementation ChannelViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    UIApplication.sharedApplication.idleTimerDisabled = YES;
    [self createEngineKit];
    [self joinChannel];
}

- (IBAction)muteAudio:(UIButton *)sender {
    sender.selected = !sender.selected;
    if (sender.selected) {
        [self.engineKit muteAudio];
    } else {
        [self.engineKit unmuteAudio];
    }
}

- (IBAction)switchSpeaker:(UIButton *)sender {
    sender.selected = !sender.selected;
    [self.engineKit setLoudspeakerStatus:sender.selected];
}

- (IBAction)exitChannel:(UIButton *)sender {
    [self leaveChannel];
    [self destroyEngineKit];
    [self dismissViewControllerAnimated:YES completion:nil];
    UIApplication.sharedApplication.idleTimerDisabled = NO;
}

- (void)createEngineKit {
    PanoRtcEngineConfig * engineConfig = [[PanoRtcEngineConfig alloc] init];
    engineConfig.appId = ChannelInfo.appId;
    engineConfig.rtcServer = ChannelInfo.server;
    self.engineKit = [PanoRtcEngineKit engineWithConfig:engineConfig delegate:self];
    engineConfig = nil;
}

- (void)destroyEngineKit {
    [self.engineKit destroy];
    self.engineKit = nil;
}

- (void)joinChannel {
    PanoRtcChannelConfig * channelConfig = [[PanoRtcChannelConfig alloc] init];
    channelConfig.mode = ChannelInfo.channelMode;
    channelConfig.userName = ChannelInfo.userName;
    PanoResult result = [_engineKit joinChannelWithToken:ChannelInfo.token
                                               channelId:ChannelInfo.channelId
                                                  userId:ChannelInfo.userId
                                                  config:channelConfig];
    if (result != kPanoResultOK) {
        self.message.text = [NSString stringWithFormat:@"Join channel failed with error: %ld.", (long)result];
    }
}

- (void)leaveChannel {
    [self.engineKit leaveChannel];
}

- (void)onChannelJoinConfirm:(PanoResult)result {
    dispatch_async(dispatch_get_main_queue(), ^{
        if (result == kPanoResultOK) {
            self.message.text = [NSString stringWithFormat:@"Join channel successfully."];
            [self.engineKit setLoudspeakerStatus:YES];
            [self.engineKit startAudio];
        } else {
            self.message.text = [NSString stringWithFormat:@"Join channel failed with error: %ld.", (long)result];
        }
    });
}

- (void)onChannelLeaveIndication:(PanoResult)result {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"Leave channel with indication: %ld.", (long)result];
    });
}

- (void)onChannelFailover:(PanoFailoverState)state {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"Failover channel with state: %ld.", (long)state];
    });
}

- (void)onUserJoinIndication:(UInt64)userId
                    withName:(NSString * _Nullable)userName {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) join channel.", userName, userId];
    });
}

- (void)onUserLeaveIndication:(UInt64)userId
                   withReason:(PanoUserLeaveReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) leave channel with reason: %ld.", nil, userId, reason];
    });
}

- (void)onUserAudioStart:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) start audio.", nil, userId];
    });
}

- (void)onUserAudioStop:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) stop audio.", nil, userId];
    });
}

- (void)onUserAudioMute:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) mute audio.", nil, userId];
    });
}

- (void)onUserAudioUnmute:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %@ (%lld) unmute audio.", nil, userId];
    });
}

@end
