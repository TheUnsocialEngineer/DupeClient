package com.dupeclient.client.module.dupedb.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;

final class SuggestionDropdown {
   private static final int MAX_VISIBLE = 10;
   private static final int ROW_H = 12;
   private static final int MIN_WIDTH = 120;
   private final EditBox field;
   private final Font tr;
   private final Supplier<List<SuggestionDropdown.Item>> source;
   private List<SuggestionDropdown.Item> filtered = new ArrayList<>();
   private int highlight;
   private int scroll;
   private String lastQuery;
   private int popupX;
   private int popupY;
   private int popupW;
   private int visibleCount;

   SuggestionDropdown(EditBox field, Font tr, Supplier<List<SuggestionDropdown.Item>> source) {
      this.field = field;
      this.tr = tr;
      this.source = source;
   }

   EditBox field() {
      return this.field;
   }

   boolean isOpen() {
      return this.field.visible && this.field.isFocused();
   }

   boolean hasSuggestions() {
      return !this.filtered.isEmpty();
   }

   void refresh() {
      String query = this.field.getValue() == null ? "" : this.field.getValue().trim().toLowerCase(Locale.ROOT);
      List<SuggestionDropdown.Item> base = this.source.get();
      List<SuggestionDropdown.Item> next = new ArrayList<>();
      if (query.isEmpty()) {
         next.addAll(base);
      } else {
         for (SuggestionDropdown.Item it : base) {
            String hay = it.searchText() != null ? it.searchText() : it.label();
            if (hay != null && hay.toLowerCase(Locale.ROOT).contains(query)) {
               next.add(it);
            }
         }
      }

      this.filtered = next;
      if (!query.equals(this.lastQuery)) {
         this.highlight = 0;
         this.scroll = 0;
         this.lastQuery = query;
      }

      if (this.highlight >= this.filtered.size()) {
         this.highlight = Math.max(0, this.filtered.size() - 1);
      }

      this.clampScroll();
   }

   private void clampScroll() {
      int maxScroll = Math.max(0, this.filtered.size() - 10);
      this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
   }

   private void ensureHighlightVisible() {
      if (this.highlight < this.scroll) {
         this.scroll = this.highlight;
      } else if (this.highlight >= this.scroll + 10) {
         this.scroll = this.highlight - 10 + 1;
      }
   }

   void moveHighlight(int delta) {
      if (!this.filtered.isEmpty()) {
         this.highlight = Math.floorMod(this.highlight + delta, this.filtered.size());
         this.ensureHighlightVisible();
         this.clampScroll();
      }
   }

   boolean selectHighlighted() {
      if (this.highlight >= 0 && this.highlight < this.filtered.size()) {
         this.apply(this.filtered.get(this.highlight));
         return true;
      } else {
         return false;
      }
   }

   void close() {
      this.field.setFocused(false);
   }

   private void apply(SuggestionDropdown.Item it) {
      this.field.setValue(it.value());
      this.field.setFocused(false);
      this.lastQuery = null;
   }

   boolean scroll(double verticalAmount) {
      if (this.filtered.size() <= 10) {
         return false;
      } else {
         if (verticalAmount < 0.0) {
            this.scroll++;
         } else if (verticalAmount > 0.0) {
            this.scroll--;
         }

         int maxScroll = Math.max(0, this.filtered.size() - 10);
         this.scroll = Math.max(0, Math.min(maxScroll, this.scroll));
         if (this.highlight < this.scroll) {
            this.highlight = this.scroll;
         } else if (this.highlight >= this.scroll + 10) {
            this.highlight = this.scroll + 10 - 1;
         }

         return true;
      }
   }

   boolean isMouseOverPopup(double mx, double my) {
      if (this.isOpen() && this.visibleCount != 0) {
         int h = this.visibleCount * 12;
         return mx >= this.popupX && mx <= this.popupX + this.popupW && my >= this.popupY && my <= this.popupY + h;
      } else {
         return false;
      }
   }

   boolean onClick(double mx, double my) {
      if (!this.isMouseOverPopup(mx, my)) {
         return false;
      } else {
         int row = (int)((my - this.popupY) / 12.0);
         int idx = this.scroll + row;
         if (idx >= 0 && idx < this.filtered.size()) {
            this.highlight = idx;
            this.apply(this.filtered.get(idx));
            return true;
         } else {
            return false;
         }
      }
   }

   void render(GuiGraphics ctx) {
      this.refresh();
      if (this.filtered.isEmpty()) {
         this.visibleCount = 0;
      } else {
         int w = Math.max(120, this.field.getWidth());
         int x = this.field.getX();
         int y = this.field.getY() + this.field.getHeight();
         int count = Math.min(10, this.filtered.size());
         this.popupX = x;
         this.popupY = y;
         this.popupW = w;
         this.visibleCount = count;
         int h = count * 12;
         ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, -15657697);
         ctx.fill(x, y, x + w, y + h, -266723021);

         for (int i = 0; i < count; i++) {
            int idx = this.scroll + i;
            if (idx >= this.filtered.size()) {
               break;
            }

            SuggestionDropdown.Item it = this.filtered.get(idx);
            int ry = y + i * 12;
            if (idx == this.highlight) {
               ctx.fill(x, ry, x + w, ry + 12, -13877661);
            }

            String countText = this.formatCount(it.count());
            int countW = this.tr.width(countText);
            String label = this.trimToWidth(it.label(), Math.max(0, w - countW - 12));
            ctx.drawString(this.tr, label, x + 4, ry + 2, -1642753, false);
            ctx.drawString(this.tr, countText, x + w - countW - 4, ry + 2, -6442794, false);
         }

         if (this.filtered.size() > count) {
            int trackTop = y + 1;
            int trackBottom = y + h - 1;
            int trackH = Math.max(1, trackBottom - trackTop);
            int maxScroll = this.filtered.size() - count;
            int thumbH = Math.max(4, trackH * count / this.filtered.size());
            int thumbY = trackTop + (maxScroll == 0 ? 0 : (trackH - thumbH) * this.scroll / maxScroll);
            ctx.fill(x + w - 2, thumbY, x + w - 1, thumbY + thumbH, -11177825);
         }
      }
   }

   private String formatCount(long count) {
      return String.format(Locale.US, "(%,d)", count);
   }

   private String trimToWidth(String value, int maxWidth) {
      if (value == null) {
         return "";
      } else if (maxWidth > 0 && this.tr.width(value) > maxWidth) {
         String ellipsis = "...";
         String trimmed = this.tr.plainSubstrByWidth(value, Math.max(0, maxWidth - this.tr.width(ellipsis)));
         return trimmed + ellipsis;
      } else {
         return value;
      }
   }

   record Item(String value, String label, long count, String searchText) {
   }
}
