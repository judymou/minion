package com.judyian.minion;

import java.io.IOException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Menu;
import android.view.SurfaceView;

public class MainActivityWithService extends Activity {
	private WakeLock wakeLock;
	public static SurfaceView surface;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		System.out.println("Oncreate MainActivityWithService");
		setContentView(R.layout.activity_main);
		surface = (SurfaceView) findViewById(R.id.surfaceView);
		
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Minion Wake Lock");
		wakeLock.acquire();
		
		Intent intent = new Intent(this, MainService.class);
		startService(intent);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		System.out.println("Android is destroying us.....");
		stopService(new Intent(this, MainService.class));
		wakeLock.release();
	}
}
