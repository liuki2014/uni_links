package name.avioli.unilinks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
public class UniLinksPlugin implements FlutterPlugin, 
        MethodChannel.MethodCallHandler,
        EventChannel.StreamHandler,
        ActivityAware {
    private static final String MESSAGES_CHANNEL = "uni_links/messages";
    private static final String EVENTS_CHANNEL = "uni_links/events";
    private BroadcastReceiver changeReceiver;
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private Context applicationContext;
    private ActivityPluginBinding activityBinding;
    private String initialLink;
    private String latestLink;
    private boolean initialIntent = true;
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        this.applicationContext = binding.getApplicationContext();
        BinaryMessenger messenger = binding.getBinaryMessenger();
        methodChannel = new MethodChannel(messenger, MESSAGES_CHANNEL);
        methodChannel.setMethodCallHandler(this);
        eventChannel = new EventChannel(messenger, EVENTS_CHANNEL);
        eventChannel.setStreamHandler(this);
    }
    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
        applicationContext = null;
    }
    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        this.activityBinding = binding;
        binding.addOnNewIntentListener(this::onNewIntent);
        handleIntent(applicationContext, binding.getActivity().getIntent());
    }
    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }
    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }
    @Override
    public void onDetachedFromActivity() {
        if (activityBinding != null) {
            activityBinding.removeOnNewIntentListener(this::onNewIntent);
            activityBinding = null;
        }
    }
    // region Intent Handling
    private void handleIntent(Context context, Intent intent) {
        String action = intent.getAction();
        String dataString = intent.getDataString();
        if (Intent.ACTION_VIEW.equals(action)) {
            if (initialIntent) {
                initialLink = dataString;
                initialIntent = false;
            }
            latestLink = dataString;
            if (changeReceiver != null) changeReceiver.onReceive(context, intent);
        }
    }
    private boolean onNewIntent(Intent intent) {
        handleIntent(applicationContext, intent);
        return false;
    }
    // endregion
    // region Broadcast Receiver
    @NonNull
    private BroadcastReceiver createChangeReceiver(final EventChannel.EventSink events) {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String dataString = intent.getDataString();
                if (dataString == null) {
                    events.error("UNAVAILABLE", "Link unavailable", null);
                } else {
                    events.success(dataString);
                }
            }
        };
    }
    // endregion
    // region Channel Handlers
    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        changeReceiver = createChangeReceiver(events);
    }
    @Override
    public void onCancel(Object arguments) {
        changeReceiver = null;
    }
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        if (call.method.equals("getInitialLink")) {
            result.success(initialLink);
        } else if (call.method.equals("getLatestLink")) {
            result.success(latestLink);
        } else {
            result.notImplemented();
        }
    }
    // endregion
}
