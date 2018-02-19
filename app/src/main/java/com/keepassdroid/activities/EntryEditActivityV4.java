/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.keepassdroid.app.App;
import com.keepassdroid.database.PwDatabaseV4;
import com.keepassdroid.database.PwEntry;
import com.keepassdroid.database.PwEntryV4;
import com.keepassdroid.database.PwGroupId;
import com.keepassdroid.database.PwGroupIdV4;
import com.keepassdroid.database.PwGroupV4;
import com.keepassdroid.database.security.ProtectedString;
import com.keepassdroid.utils.Types;
import com.keepassdroid.view.EntryEditNewField;
import com.kunzisoft.keepass.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

public class EntryEditActivityV4 extends EntryEditActivity {
	
	private ScrollView scroll;

	protected static void putParentId(Intent i, String parentKey, PwGroupV4 parent) {
		PwGroupId id = parent.getId();
		PwGroupIdV4 id4 = (PwGroupIdV4) id;
		
		i.putExtra(parentKey, Types.UUIDtoBytes(id4.getId()));
		
	}

	@Override
	protected PwGroupId getParentGroupId(Intent i, String key) {
		byte[] buf = i.getByteArrayExtra(key);
		UUID id = Types.bytestoUUID(buf);
		
		return new PwGroupIdV4(id);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		scroll = (ScrollView) findViewById(R.id.entry_scroll);
		
		View add = findViewById(R.id.add_new_field);
		add.setVisibility(View.VISIBLE);
		add.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				LinearLayout container = (LinearLayout) findViewById(R.id.advanced_container);
				
				EntryEditNewField ees = new EntryEditNewField(EntryEditActivityV4.this);
				ees.setData("", new ProtectedString(false, ""));
				container.addView(ees);
				
				// Scroll bottom
				scroll.post(new Runnable() {
					@Override
					public void run() {
						scroll.fullScroll(ScrollView.FOCUS_DOWN);
					}
				});
			}
		});
	}

	@Override
	protected void fillData() {
		super.fillData();
		
		PwEntryV4 entry = (PwEntryV4) mEntry;
		
		LinearLayout container = (LinearLayout) findViewById(R.id.advanced_container);
		
		if (entry.strings.size() > 0) {
			for (Entry<String, ProtectedString> pair : entry.strings.entrySet()) {
				String key = pair.getKey();
				
				if (!PwEntryV4.IsStandardString(key)) {
					EntryEditNewField ees = new EntryEditNewField(EntryEditActivityV4.this);
					ees.setData(key, pair.getValue());
					container.addView(ees);
				}
			}
		}
		
	}

	@SuppressWarnings("unchecked")
	@Override
	protected PwEntry populateNewEntry() {
		PwEntryV4 newEntry = (PwEntryV4) mEntry.clone(true);
		newEntry.history = (ArrayList<PwEntryV4>) newEntry.history.clone();
		newEntry.createBackup((PwDatabaseV4)App.getDB().pm);
		
		newEntry = (PwEntryV4) super.populateNewEntry(newEntry);
		
		Map<String, ProtectedString> strings = newEntry.strings;
		
		// Delete all new standard strings
		Iterator<Entry<String, ProtectedString>> iter = strings.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, ProtectedString> pair = iter.next();
			if (!PwEntryV4.IsStandardString(pair.getKey())) {
				iter.remove();
			}
		}
		
		LinearLayout container = (LinearLayout) findViewById(R.id.advanced_container);
		for (int i = 0; i < container.getChildCount(); i++) {
			EntryEditNewField view = (EntryEditNewField) container.getChildAt(i);
			String key = view.getLabel();
			String value = view.getValue();
			boolean protect = view.isProtected();
			strings.put(key, new ProtectedString(protect, value));
		}
		
		return newEntry;
	}

	@Override
	protected boolean validateBeforeSaving() {
		if(!super.validateBeforeSaving()) {
			return false;
		}

		ViewGroup container = (ViewGroup) findViewById(R.id.advanced_container);
		for (int i = 0; i < container.getChildCount(); i++) {
			EntryEditNewField ees = (EntryEditNewField) container.getChildAt(i);
			String key = ees.getLabel();
			if (key == null || key.length() == 0) {
				Toast.makeText(this, R.string.error_string_key, Toast.LENGTH_LONG).show();
				return false;
			}
		}
		
		return true;
	}

}
 