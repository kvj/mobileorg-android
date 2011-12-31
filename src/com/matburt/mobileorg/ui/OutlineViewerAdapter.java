package com.matburt.mobileorg.ui;

import java.io.StringWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
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

import com.matburt.mobileorg.R;
import com.matburt.mobileorg.service.DataController;
import com.matburt.mobileorg.service.DataController.TodoState;
import com.matburt.mobileorg.service.DataWriter;
import com.matburt.mobileorg.service.NoteNG;
import com.matburt.mobileorg.service.OrgNGParser;
import com.matburt.mobileorg.ui.theme.Default;

public class OutlineViewerAdapter implements ListAdapter {

	Integer id = null;
	NoteNG clicked = null;
	static Default theme = new Default();
	static int[] levelColors = new int[0];
	static Map<Integer, Integer> tagMapping = new HashMap<Integer, Integer>();
	PlainTextFormatter textFormatter = null;
	PlainTextFormatter sublistFormatter = null;

	static {
		levelColors = new int[] { theme.ccLBlue, theme.c3Yellow, theme.ceLCyan,
				theme.c1Red, theme.c2Green, theme.c5Purple, theme.ccLBlue,
				theme.c2Green, theme.ccLBlue, theme.c3Yellow, theme.ceLCyan };
		tagMapping.put(theme.c1Red, theme.c9LRed);
		tagMapping.put(theme.c2Green, theme.caLGreen);
		tagMapping.put(theme.c3Yellow, theme.cbLYellow);
		tagMapping.put(theme.c4Blue, theme.ccLBlue);
		tagMapping.put(theme.c5Purple, theme.cdLPurple);
		tagMapping.put(theme.c6Cyan, theme.ceLCyan);
		tagMapping.put(theme.c7White, theme.cfLWhite);
	}

	public OutlineViewerAdapter(Context context) {
		wide = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		DateTextFormatter dateTextFormatter = new DateTextFormatter();
		CheckBoxFormatter checkBoxFormatter = new CheckBoxFormatter();
		textFormatter = new PlainTextFormatter(dateTextFormatter);
		sublistFormatter = new PlainTextFormatter(checkBoxFormatter,
				dateTextFormatter);
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
					buffer.setSpan(span[i], start, end,
							SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}
		}
	}

	interface TextFormatter {
		Pattern getPattern(NoteNG note, boolean selected);

		void format(NoteNG note, SpannableStringBuilder sb, Matcher m,
				String text, boolean selected);
	}

	class CheckBoxFormatter implements TextFormatter {

		@Override
		public Pattern getPattern(NoteNG note, boolean selected) {
			if (!NoteNG.TYPE_SUBLIST.equals(note.type)) {
				return null;
			}
			return OrgNGParser.checkboxPattern;
		}

		@Override
		public void format(NoteNG note, SpannableStringBuilder sb, Matcher m,
				String text, boolean selected) {
			note.checkboxState = "X".equals(m.group(1)) ? NoteNG.CBOX_CHECKED
					: NoteNG.CBOX_UNCHECKED;
			// Log.i(TAG, "Ckeckbox detected[" + selected + "]: " + m.group()
			// + ", " + m.group(1));
			// if (!selected) {
			// addSpan(sb, text);
			// } else {
			addSpan(sb, text, new BackgroundColorSpan(theme.c7White),
					new ForegroundColorSpan(theme.cfLWhite));
			// }
		}

	}

	class DateTextFormatter implements TextFormatter {

		@Override
		public Pattern getPattern(NoteNG note, boolean selected) {
			return OrgNGParser.dateTimePattern;
		}

		@Override
		public void format(NoteNG note, SpannableStringBuilder sb, Matcher m,
				String text, boolean selected) {
			// OrgNGParser.debugExp(m);
			StringBuilder builder = new StringBuilder();
			builder.append(m.group(1));
			try {
				DateFormat dateFormat = android.text.format.DateFormat
						.getDateFormat(controller.getContext());
				Date date = DataController.dateFormat.parse(m.group(2));
				builder.append(dateFormat.format(date));
				if (null != m.group(3)) {
					DateFormat timeFormat = android.text.format.DateFormat
							.getTimeFormat(controller.getContext());
					date = DataController.timeFormat.parse(m.group(3));
					builder.append(' ');
					builder.append(timeFormat.format(date));
				}
				builder.append(m.group(4));
				addSpan(sb, builder.toString(), new ForegroundColorSpan(
						theme.c5Purple));
			} catch (Exception e) {
				e.printStackTrace();
				addSpan(sb, "Error!", new ForegroundColorSpan(theme.c9LRed));
			}

		}

	}

	class PlainTextFormatter {

		TextFormatter[] formatters;

		public PlainTextFormatter(TextFormatter... formatters) {
			this.formatters = formatters;
		}

		private void writePlainText(NoteNG note, SpannableStringBuilder sb,
				int defColor, String text, int index, boolean selected) {
			if (index >= formatters.length) {
				addSpan(sb, text, new ForegroundColorSpan(defColor));
				return;
			}
			TextFormatter formatter = formatters[index];
			Matcher m = null;
			Pattern p = formatter.getPattern(note, selected);
			if (null != p) {
				m = p.matcher(text);
			}
			if (null == m || !m.find()) {
				writePlainText(note, sb, defColor, text, index + 1, selected);
				return;
			}
			do {
				StringBuffer buffer = new StringBuffer();
				m.appendReplacement(buffer, "");
				if (0 != buffer.length()) {
					writePlainText(note, sb, defColor, buffer.toString(),
							index + 1, selected);
				}
				formatter.format(note, sb, m, m.group(), selected);
			} while (m.find());
			StringBuffer buffer = new StringBuffer();
			m.appendTail(buffer);
			if (0 != buffer.length()) {
				writePlainText(note, sb, defColor, buffer.toString(),
						index + 1, selected);
			}
		}

		void writePlainText(NoteNG note, SpannableStringBuilder sb,
				int defColor, String text, boolean selected) {
			writePlainText(note, sb, defColor, text, 0, selected);
		}

	}

	public class TextViewParts {
		public CharSequence leftPart = null;
		public CharSequence rightPart = null;
	}

	public TextViewParts customizeTextView(NoteNG note, boolean isclicked) {
		TextViewParts result = new TextViewParts();
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
		if (wide && null != note.before
				&& !NoteNG.TYPE_SUBLIST.equals(note.type)) {
			addSpan(sb, note.before, new ForegroundColorSpan(theme.c3Yellow));
		}
		if (null != note.todo) {
			Boolean done = todos.get(note.todo);
			if (null == done) {
				done = false;
			}
			addSpan(sb, note.todo + ' ', new ForegroundColorSpan(
					done ? theme.c9LRed : theme.c1Red));
			// sb.append(note.todo+' ');
		}

		int titleColor = theme.c7White;
		if (NoteNG.TYPE_AGENDA.equals(note.type)) {
			titleColor = theme.ccLBlue;
		}
		if (NoteNG.TYPE_OUTLINE.equals(note.type)) {
			titleColor = levelColors[(note.level - 1) % levelColors.length];
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
			addSpan(sb, "[#" + note.priority + "]", new ForegroundColorSpan(
					prColor), priority > 1 ? new UnderlineSpan() : null);
			addSpan(sb, " ");
		}
		if (NoteNG.TYPE_SUBLIST.equals(note.type)
				|| NoteNG.TYPE_TEXT.equals(note.type)) {
			StringWriter sw = new StringWriter();
			if (null != note.before) {
				try {
					DataWriter.writeIndent(1 + note.before.length(), sw);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			String subListIndent = sw.toString();
			String[] lines = note.raw.split("\\n");
			for (int i = 0; i < lines.length; i++) {
				if (i == 0) {
					if (null != note.before) {
						addSpan(sb, note.before + ' ');
					}
				} else {
					addSpan(sb, '\n' + indent + subListIndent);
				}
				sublistFormatter.writePlainText(note, sb, theme.c7White,
						lines[i], isclicked);
				// addSpan(sb, lines[i]);
			}
		} else {
			textFormatter.writePlainText(note, sb, titleColor, note.title,
					isclicked);
			// addSpan(sb, note.title, new ForegroundColorSpan(
			// titleColor));
		}
		if (null != note.tags) {
			SpannableStringBuilder tagsText = new SpannableStringBuilder();
			Integer tagColor = tagMapping.get(titleColor);
			if (null == tagColor) {
				tagColor = titleColor;
			}
			int size = sb.length();
			addSpan(tagsText, note.tags, new ForegroundColorSpan(tagColor));
			result.rightPart = tagsText;
		}
		if (isclicked) {
			sb.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, sb.length(), 0);
		}
		result.leftPart = sb;
		return result;
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
		// boolean isselected = selected == note.id;
		boolean isclicked = null != clicked && clicked.id.equals(note.id);
		TextView title = (TextView) convertView
				.findViewById(R.id.outline_viewer_item_text);
		TextView tags = (TextView) convertView
				.findViewById(R.id.outline_viewer_item_tags);
		// convertView.setBackgroundColor(isselected
		// ? theme.c2Green
		// : isclicked
		// ? theme.c7White
		// : theme.c0Black);
		TextViewParts parts = customizeTextView(note, isclicked);
		title.setText(parts.leftPart, BufferType.SPANNABLE);
		tags.setText(parts.rightPart == null ? "" : parts.rightPart,
				BufferType.SPANNABLE);
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

	public int setController(Integer id, DataController controller,
			List<Integer> selection) {
		if (null == this.controller) {
			this.controller = controller;
		}
		this.id = id;
		if (null != id && -1 == id) {
			this.id = null;
		}
		return reload(selection);
	}

	public int reload(List<Integer> selection) {
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
			if (null == selection) {
				selection = new ArrayList<Integer>();
				selection.add(root.id);
			}
		} else {
			List<NoteNG> _list = controller.getData(id);
			if (null != _list) {
				data.addAll(_list);
			}
		}
		int selectedPos = -1;
		// Log.i(TAG, "Reload with: "+selection);
		if (null != selection) {
			int start = 0;
			int end = data.size();
			for (int i = 0; i < selection.size(); i++) {
				Integer id = selection.get(i);
				boolean found = false;
				for (int j = start; j < end; j++) {
					NoteNG n = data.get(j);
					if (n.id.equals(id)) {
						// Found
						clicked = n;
						selectedPos = j;
						start = j + 1;
						end = expandNote(n, j, false) + start;
						found = true;
						break;
					}
				}
				if (!found) {
					break;
				}
			}
		}
		if (null != observer) {
			observer.onChanged();
		}
		return selectedPos;
	}

	public void setExpanded(int position, int state) {
		if (-1 == position) {
			return;
		}
		NoteNG note = getItem(position);
		collapseNote(note, position);
		if (NoteNG.EXPAND_COLLAPSED != state) {
			expandNote(note, position, NoteNG.EXPAND_MANY == state ? true
					: false);
		}
	}

	public void collapseExpand(int position, boolean notify) {
		NoteNG note = getItem(position);
		if (!note.isExpandable()) {
			clicked = note;
			if (notify && null != observer) {
				observer.onChanged();
			}
			return;
		}
		if (note.expanded == NoteNG.EXPAND_COLLAPSED) {
			expandNote(note, position, false);
		} else {
			if (null != clicked && clicked.id.equals(note.id)) {
				if (note.expanded == NoteNG.EXPAND_ONE) {
					collapseNote(note, position);
					expandNote(note, position, true);
				} else {
					collapseNote(note, position);
				}
			}
		}
		clicked = note;
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

	public int expandNote(NoteNG note, int position, boolean expandAll) {
		note.expanded = expandAll ? NoteNG.EXPAND_MANY : NoteNG.EXPAND_ONE;
		List<NoteNG> list = controller.getData(note.id);
		if (null == list) {
			return 0;
		}
		int pos = 0;
		for (int i = 0; i < list.size(); i++) {
			NoteNG n = list.get(i);
			n.parentNote = note;
			n.index = i;
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
			return "id:" + noteID;
		}
		return noteID;
	}

	public void setShowWide(boolean wide) {
		this.wide = wide;
		if (null != observer) {
			observer.onChanged();
		}
	}

	ArrayList<Integer> getSelection() {
		if (clicked == null) {
			return null;
		}
		ArrayList<Integer> result = new ArrayList<Integer>();
		NoteNG note = clicked;
		while (null != note) {
			result.add(0, note.id);
			note = note.parentNote;
		}
		return result;
	}

	public NoteNG findNearestNote(NoteNG note) {
		do {
			if (null == note) {
				return null;
			}
			if (NoteNG.TYPE_OUTLINE.equals(note.type)) {
				// Found - return
				return note;
			}
			// if (NoteNG.TYPE_AGENDA_OUTLINE.equals(note.type) && null !=
			// note.originalID) {
			// //We are in outline - try to jump
			// NoteNG n = controller.findNoteByNoteID(note.originalID);
			// if (null != note) {
			// note = n;
			// continue;
			// }
			// }
			note = note.parentNote;
		} while (true);
	}

	public int findItem(Integer id) {
		synchronized (data) {
			for (int i = 0; i < data.size(); i++) {
				if (data.get(i).id.equals(id)) {
					return i;
				}
			}
		}
		return -1;
	}

	public void setSelected(int pos) {
		if (-1 == pos) {
			return;
		}
		clicked = getItem(pos);
		if (null != observer) {
			observer.onChanged();
		}
	}

	public void setWide(boolean wide) {
		this.wide = wide;
	}

	public void notifyChanged() {
		if (null != observer) {
			observer.onChanged();
		}
	}

}
