package yuku.alkitab.base.widget;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

import yuku.afw.V;
import yuku.afw.storage.Preferences;
import yuku.alkitab.base.App;
import yuku.alkitab.base.S;
import yuku.alkitab.base.U;
import yuku.alkitab.base.util.Appearances;
import yuku.alkitab.base.util.TargetDecoder;
import yuku.alkitab.debug.R;
import yuku.alkitab.model.PericopeBlock;
import yuku.alkitab.util.Ari;
import yuku.alkitab.util.IntArrayList;


public class SingleViewVerseAdapter extends VerseAdapter {
	public static final String TAG = SingleViewVerseAdapter.class.getSimpleName();
	
	public SingleViewVerseAdapter(Context context) {
		super(context);
	}

	@Override public synchronized View getView(int position, View convertView, ViewGroup parent) {
		// Need to determine this is pericope or verse
		int id = itemPointer_[position];

		if (id >= 0) {
			// VERSE. not pericope
			int verse_1 = id + 1;

			boolean checked = false;
			if (parent instanceof ListView) {
				checked = ((ListView) parent).isItemChecked(position);
			}

			final VerseItem res;
			if (convertView == null || convertView.getId() != R.id.itemVerse) {
				res = (VerseItem) inflater_.inflate(R.layout.item_verse, parent, false);
			} else {
				res = (VerseItem) convertView;
			}

			final VerseTextView lText = V.get(res, R.id.lText);
			final TextView lVerseNumber = V.get(res, R.id.lVerseNumber);

			final int ari = Ari.encode(book_.bookId, chapter_1_, verse_1);
			final String text = verses_.getVerse(id);
			final String verseNumberText = verses_.getVerseNumberText(id);
			final boolean dontPutSpacingBefore = (position > 0 && itemPointer_[position - 1] < 0) || position == 0;
			final int highlightColor = (highlightColorMap_ != null && highlightColorMap_[id] != -1) ? U.alphaMixHighlight(highlightColorMap_[id]) : -1;

			VerseRenderer.render(lText, lVerseNumber, ari, text, verseNumberText, highlightColor, checked, dontPutSpacingBefore, inlineLinkSpanFactory_, owner_);

			Appearances.applyTextAppearance(lText);
			if (checked) {
				lText.setTextColor(0xff000000); // override with black!
			}

			final AttributeView attributeView = (AttributeView) res.findViewById(R.id.view_attributes);
			attributeView.setBookmarkCount(bookmarkCountMap_ == null ? 0 : bookmarkCountMap_[id]);
			attributeView.setNoteCount(noteCountMap_ == null ? 0 : noteCountMap_[id]);
			attributeView.setProgressMarkBits(progressMarkBitsMap_ == null ? 0 : progressMarkBitsMap_[id]);
			attributeView.setAttributeListener(attributeListener_, book_, chapter_1_, verse_1);

			try { // Catch bad Xrefs.
				res.setCollapsed(text.length() == 0 && !attributeView.isShowingSomething());
			} catch (Exception e) {
				Log.e(TAG, App.context.getString(R.string.xref_missingverse));
                Toast.makeText( this.context_, App.context.getString(R.string.xref_missingverse), Toast.LENGTH_SHORT).show();
				//return null; // FIXME probably needs to handle this further on...
			}

			res.setAri(ari);

//			{ // DUMP
//				Log.d(TAG, "==== DUMP verse " + (id + 1));
//				SpannedString sb = (SpannedString) lText.getText();
//				Object[] spans = sb.getSpans(0, sb.length(), Object.class);
//				for (Object span: spans) {
//					int start = sb.getSpanStart(span);
//					int end = sb.getSpanEnd(span);
//					Log.d(TAG, "Span " + span.getClass().getSimpleName() + " " + start + ".." + end + ": " + sb.toString().substring(start, end));
//				}
//			}

			return res;
		} else {
			// PERICOPE. not verse.

			final PericopeHeaderItem res;
			if (convertView == null || convertView.getId() != R.id.itemPericopeHeader) {
				res = (PericopeHeaderItem) inflater_.inflate(R.layout.item_pericope_header, parent, false);
			} else {
				res = (PericopeHeaderItem) convertView;
			}

			PericopeBlock pericopeBlock = pericopeBlocks_[-id - 1];

			TextView lCaption = (TextView) res.findViewById(R.id.lCaption);
			TextView lParallels = (TextView) res.findViewById(R.id.lParallels);

			lCaption.setText(FormattedTextRenderer.render(pericopeBlock.title));

			int paddingTop;
			// turn off top padding if the position == 0 OR before this is also a pericope title
			if (position == 0 || itemPointer_[position - 1] < 0) {
				paddingTop = 0;
			} else {
				paddingTop = S.applied.pericopeSpacingTop;
			}

			res.setPadding(0, paddingTop, 0, S.applied.pericopeSpacingBottom);

			Appearances.applyPericopeTitleAppearance(lCaption);

			// make parallel gone if not exist
			if (pericopeBlock.parallels.length == 0) {
				lParallels.setVisibility(View.GONE);
			} else {
				lParallels.setVisibility(View.VISIBLE);

				SpannableStringBuilder sb = new SpannableStringBuilder("("); //$NON-NLS-1$

				int total = pericopeBlock.parallels.length;
				for (int i = 0; i < total; i++) {
					String parallel = pericopeBlock.parallels[i];

					if (i > 0) {
						// force new line for certain parallel patterns
						if ((total == 6 && i == 3) || (total == 4 && i == 2) || (total == 5 && i == 3)) {
							sb.append("; \n"); //$NON-NLS-1$
						} else {
							sb.append("; "); //$NON-NLS-1$
						}
					}

                    appendParallel(sb, parallel);
				}
				sb.append(')');

				lParallels.setText(sb, BufferType.SPANNABLE);
				Appearances.applyPericopeParallelTextAppearance(lParallels);
			}

			return res;
		}
	}

	private void appendParallel(SpannableStringBuilder sb, String parallel) {
        int sb_len = sb.length();

        linked: {
            if (parallel.startsWith("@")) {
	            // look for the end
	            int targetEndPos = parallel.indexOf(' ', 1);
	            if (targetEndPos == -1) {
		            break linked;
	            }

	            final String target = parallel.substring(1, targetEndPos);
	            final IntArrayList ariRanges = TargetDecoder.decode(target);
	            if (ariRanges == null || ariRanges.size() == 0) {
		            break linked;
	            }

	            final String display = parallel.substring(targetEndPos + 1);

                // if we reach this, data and display should have values, and we must not go to fallback below
                sb.append(display);
                sb.setSpan(new CallbackSpan<>(ariRanges.get(0), parallelListener_), sb_len, sb.length(), 0);
                return; // do not remove this
            }
        }

        // fallback if the above code fails
        sb.append(parallel);
        sb.setSpan(new CallbackSpan<>(parallel, parallelListener_), sb_len, sb.length(), 0);
    }
}
