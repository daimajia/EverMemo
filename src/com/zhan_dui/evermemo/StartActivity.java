package com.zhan_dui.evermemo;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Window;
import android.widget.TextView;

import com.huewu.pla.lib.MultiColumnListView;
import com.zhan_dui.adapters.MemosAdapter;
import com.zhan_dui.data.MemoProvider;
import com.zhan_dui.data.MemoDB;

public class StartActivity extends FragmentActivity implements
		LoaderCallbacks<Cursor> {

	private TextView mEverTextView;
	private TextView mMemoTextView;
	private MultiColumnListView mMemosGrid;
	private MemoDB mMemoDB;
	private Context mContext;
	private MemosAdapter mMemosAdapter;
	private LayoutInflater mLayoutInflater;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		mContext = this;
		mLayoutInflater = (LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mEverTextView = (TextView) findViewById(R.id.ever);
		mMemoTextView = (TextView) findViewById(R.id.memo);
		mMemosGrid = (MultiColumnListView) findViewById(R.id.memos);

		Typeface roboto_bold = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Bold.ttf");
		Typeface roboto_thin = Typeface.createFromAsset(getAssets(),
				"fonts/Roboto-Thin.ttf");

		mEverTextView.setTypeface(roboto_bold);
		mMemoTextView.setTypeface(roboto_thin);
		LoaderManager manager = getSupportLoaderManager();
		mMemosAdapter = new MemosAdapter(mContext, null,
				CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
		mMemosGrid.setAdapter(mMemosAdapter);
		manager.initLoader(1, null, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.start, menu);
		return true;
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cursorLoader = new CursorLoader(mContext,
				MemoProvider.MEMO_URI, null, null, null, null);
		return cursorLoader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor cursor) {
		MatrixCursor matrixCursor = new MatrixCursor(new String[] { "_id" });
		matrixCursor.addRow(new String[] { "0" });
		Cursor c = new MergeCursor(new Cursor[] { matrixCursor, cursor });
		mMemosAdapter.swapCursor(c);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mMemosAdapter.swapCursor(null);
	}

}
