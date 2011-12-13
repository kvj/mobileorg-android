package com.matburt.mobileorg.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.service.DataController.TodoState;
import com.matburt.mobileorg.ui.theme.Default;

import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;

public class OutlineViewerAdapter implements ListAdapter {

	Integer id = null;
	Integer selected = null;
	Integer clicked = null;
	Default theme = null;
	int[] levelColors = new int[0];

	public OutlineViewerAdapter(Context context) {
		theme = new Default();
		wide = context.getResources().getConfiguration().orientation 
				== Configuration.ORIENTATION_LANDSCAPE;
		levelColors = new int[] {theme.ccLBlue, theme.c3Yellow, theme.ceLCyan, theme.c1Red, 
				theme.c2Green, theme.c5Purple, theme.ccLBlue, theme.c2Green, 
				theme.ccLBlue, theme.c3Yellow, theme.ceLCyan};
	}

	private static final String TAG = "OutlineView";
	List<NoteNG> data = new ArrayList<NoteNG>();
	DataSetObserver observer = null;
	DataController controller = null;
	private boolean wide = false;
	Map<String, Boolean> todos = new HashMap<String, Boolean>();
	Map<String, Integer> priorities = new HashMap<String, Integer>();

	@Override
	public int getCount() {
		return data.size();
	}

	@Override
	public NoteNG getItem(int position) {
		return data.get(position);
	}

	@Override
	public long getItemId(int position) {
		return data.get(position).id;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	public static void addSpan(SpannableStringBuilder buffer, String text,
			Object... span) {
		int start = buffer.length();
		int end = start + text.length();
		buffer.append(text);
		if (null != span) {
			for (int i = 0; i < span.length; i++) {
				if (null != span[i]) {
					buffer.setSpan(span[i], start, end, 0);
				}
			}
		}
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) parent.getContext()
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.outline_viewer_item,
					parent, false);
		}
		NoteNG note = getItem(position);
//		boolean isselected = selected == note.id;
		boolean isclicked = clicked == note.id;
		TextView title = (TextView) convertView
				.findViewById(R.id.outline_viewer_item_text);
//		convertView.setBackgroundColor(isselected 
//				? theme.c2Green 
//				: isclicked
//					? theme.c7White
//					: theme.c0Black);
		SpannableStringBuilder sb = new SpannableStringBuilder();
		String indent = "";
		if (note.indent > 0) {
			// if (selected == note.id && note.isExpandable()) {
			// sb.append(note.expanded? 'v': '>');
			// } else {
			sb.append(' ');
			// }
			for (int i = 1; i < note.indent; i++) {
				sb.append(' ');
			}
			indent = new String(sb.toString());
		}
		if (wide && null != note.before && !NoteNG.TYPE_SUBLIST.equals(note.type)) {
			addSpan(sb, note.before, new ForegroundColorSpan(theme.c3Yellow));
		}
		if (null != note.todo) {
			Boolean done = todos.get(note.todo);
			if (null == done) {
				done = false;
			}
			addSpan(sb, note.todo + ' ', 
					new ForegroundColorSpan(done? theme.c9LRed: theme.c1Red));
			// sb.append(note.todo+' ');
		}
		int titleColor = theme.c7White;
		if (NoteNG.TYPE_AGENDA.equals(note.type)) {
			titleColor = theme.ccLBlue;
		}
		if (NoteNG.TYPE_OUTLINE.equals(note.type)) {
			titleColor = levelColors[(note.level-1) % levelColors.length];
		}
		if (null != note.priority) {
			Integer priority = priorities.get(note.priority);
			if (null == priority) {
				priority = 2;
			}
			int prColor = theme.c7White;
			if (0 == priority) {
				prColor = theme.cfLWhite;
			}
			addSpan(sb, "[#" + note.priority + "]", 
					new ForegroundColorSpan(prColor), 
					priority>1? new UnderlineSpan(): null);
			addSpan(sb, " ");
		}
		if (NoteNG.TYPE_SUBLIST.equals(note.type)) {
			String[] lines = note.raw.split("\\n");
			for (int i = 0; i < lines.length; i++) {
				if (i == 0) {
					addSpan(sb, note.before+' ');
				} else {
					addSpan(sb, '\n'+indent);
				}
				addSpan(sb, lines[i]);
			}
		} else {
			addSpan(sb, note.title, new ForegroundColorSpan(
					titleColor));
		}
		if (isclicked) {
			sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), 0);
		}
		title.setText(sb, BufferType.SPANNABLE);
		return convertView;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isEmpty() {
		return getCount() == 0;
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {
		Log.i(TAG, "Register observer: " + observer);
		this.observer = observer;
	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {
		this.observer = null;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return true;
	}

	@Override
	public boolean isEnabled(int position) {
		return true;
	}

	public void setController(Integer id, DataController controller) {
		if (null == this.controller) {
			this.id = id;
			this.controller = controller;
			reload();
		}
	}

	public void reload() {
		data.clear();
		todos.clear();
		List<TodoState> todoStates = controller.getTodoTypes();
		for (TodoState t : todoStates) {
			todos.put(t.name, t.done);
		}
		priorities.clear();
		List<String> prList = controller.getPrioritiesNG();
		for (int i = 0; i < prList.size(); i++) {
			priorities.put(prList.get(i), i);
		}
		NoteNG root = controller.findNoteByID(id);
		if (null != root) {
			data.add(root);
			expandNote(root, 0, false);
		} else {
			List<NoteNG> _list = controller.getData(id);
			if (null != _list) {
				data.addAll(_list);
			}
		}
		if (null != observer) {
			observer.onChanged();
		}
	}

	public void collapseExpand(int position, boolean notify) {
		NoteNG note = getItem(position);
		clicked = note.id;
		if (!note.isExpandable()) {
			return;
		}
		selected = note.id;
		if (note.expanded == NoteNG.EXPAND_COLLAPSED) {
			expandNote(note, position, false);
		} else if (note.expanded == NoteNG.EXPAND_ONE){
			collapseNote(note, position);
			expandNote(note, position, true);
		} else {
			collapseNote(note, position);
		}
		if (notify && null != observer) {
			observer.onChanged();
		}
	}

	private void collapseNote(NoteNG note, int position) {
		note.expanded = NoteNG.EXPAND_COLLAPSED;
		int i = position + 1;
		while (i < data.size()) {
			if (data.get(i).indent <= note.indent) {
				break;
			}
			collapseNote(data.get(i), i);
			data.remove(i);
		}
	}

	private int expandNote(NoteNG note, int position, boolean expandAll) {
		note.expanded = expandAll? NoteNG.EXPAND_MANY: NoteNG.EXPAND_ONE;
		List<NoteNG> list = controller.getData(note.id);
		if (null == list) {
			return 0;
		}
		int pos = 0;
		for (int i = 0; i < list.size(); i++) {
			NoteNG n = list.get(i);
			n.indent = note.indent + 1;
			pos++;
			data.add(position + pos, n);
			if (expandAll) {
				pos += expandNote(n, position + pos, expandAll);
			}
		}
		return pos;
	}
	
	public String getIntent(int position) {
		NoteNG note = data.get(position);
		String noteID = note.originalID;
		if (null == noteID) {
			noteID = note.noteID;
		}
		if (null != noteID) {
			return "id:"+noteID;
		}
		return noteID;
	}
	
	public void setShowWide(boolean wide) {
		this.wide  = wide;
		if (null != observer) {
			observer.onChanged();
		}
	}

}
