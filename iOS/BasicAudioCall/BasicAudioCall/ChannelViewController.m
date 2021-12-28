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
@property (strong, nonatomic) NSMutableDictionary * userList;

@end

@implementation ChannelViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    UIApplication.sharedApplication.idleTimerDisabled = YES;
    self.userList = [NSMutableDictionary dictionary];
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
    channelConfig.userName = [@"iOS_" stringByAppendingString:@(ChannelInfo.userId).stringValue];
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
    [self.userList setObject:userName forKey:@(userId).stringValue];
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) join channel.", userId, userName];
    });
}

- (void)onUserLeaveIndication:(UInt64)userId
                   withReason:(PanoUserLeaveReason)reason {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) leave channel with reason: %ld.", userId, [self.userList valueForKey:@(userId).stringValue], reason];
    });
}

- (void)onUserAudioStart:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) start audio.", userId, [self.userList valueForKey:@(userId).stringValue]];
    });
}

- (void)onUserAudioStop:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) stop audio.", userId, [self.userList valueForKey:@(userId).stringValue]];
    });
}

- (void)onUserAudioMute:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) mute audio.", userId, [self.userList valueForKey:@(userId).stringValue]];
    });
}

- (void)onUserAudioUnmute:(UInt64)userId {
    dispatch_async(dispatch_get_main_queue(), ^{
        self.message.text = [NSString stringWithFormat:@"User %llu(%@) unmute audio.", userId, [self.userList valueForKey:@(userId).stringValue]];
    });
}

@end
