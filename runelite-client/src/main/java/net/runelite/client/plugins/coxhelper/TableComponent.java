package net.runelite.client.plugins.coxhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.util.Text;

/**
 * Minimal flex-width table layout for small panel overlays (2-3 columns of short strings).
 * Vanilla RuneLite has no built-in table component; OpenOSRS does
 * (com.openosrs.client.ui.overlay.components.table.TableComponent) but this plugin targets
 * vanilla RuneLite, so this is a from-scratch, deliberately small replacement covering only
 * what CoxInfoBox/CoxDebugBox actually use: per-column alignment and simple string rows,
 * with support for a single leading {@code <col=RRGGBB>} tag per cell (as produced by
 * {@link net.runelite.client.util.ColorUtil#prependColorTag}).
 */
class TableComponent implements LayoutableRenderableEntity
{
	private static final Pattern LEADING_COLOR_TAG = Pattern.compile("^<col=([0-9a-fA-F]{6})>(.*)$", Pattern.DOTALL);
	private static final int COLUMN_GUTTER = 3;

	private final List<String[]> rows = new ArrayList<>();
	private TableAlignment[] columnAlignments = new TableAlignment[0];
	private Color defaultColor = Color.WHITE;
	private Point preferredLocation = new Point();
	private Dimension preferredSize = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);
	private final Rectangle bounds = new Rectangle();

	void setColumnAlignments(TableAlignment... alignments)
	{
		this.columnAlignments = alignments;
	}

	void addRow(String... cells)
	{
		this.rows.add(cells);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (rows.isEmpty())
		{
			bounds.setLocation(preferredLocation);
			bounds.setSize(0, 0);
			return new Dimension(0, 0);
		}

		final FontMetrics metrics = graphics.getFontMetrics();
		final int columnCount = rows.stream().mapToInt(r -> r.length).max().orElse(0);
		final int[] columnWidths = columnWidths(metrics, columnCount);

		final int rowHeight = metrics.getHeight();
		int y = preferredLocation.y;

		for (String[] row : rows)
		{
			int x = preferredLocation.x;
			y += rowHeight;

			for (int col = 0; col < row.length; col++)
			{
				final Cell cell = Cell.parse(row[col]);
				final int textWidth = metrics.stringWidth(cell.text);
				final TableAlignment alignment = col < columnAlignments.length ? columnAlignments[col] : TableAlignment.LEFT;
				final int offset;
				switch (alignment)
				{
					case RIGHT:
						offset = columnWidths[col] - textWidth;
						break;
					case CENTER:
						offset = (columnWidths[col] - textWidth) / 2;
						break;
					default:
						offset = 0;
				}

				final TextComponent textComponent = new TextComponent();
				textComponent.setPosition(new Point(x + Math.max(offset, 0), y));
				textComponent.setText(cell.text);
				textComponent.setColor(cell.color != null ? cell.color : defaultColor);
				textComponent.render(graphics);

				x += columnWidths[col] + COLUMN_GUTTER;
			}
		}

		final Dimension dimension = new Dimension(preferredSize.width, y - preferredLocation.y);
		bounds.setLocation(preferredLocation);
		bounds.setSize(dimension);
		return dimension;
	}

	private int[] columnWidths(FontMetrics metrics, int columnCount)
	{
		final int[] widths = new int[columnCount];
		for (String[] row : rows)
		{
			for (int col = 0; col < row.length; col++)
			{
				final int width = metrics.stringWidth(Text.removeTags(row[col]));
				widths[col] = Math.max(widths[col], width);
			}
		}
		return widths;
	}

	@Override
	public Rectangle getBounds()
	{
		return bounds;
	}

	@Override
	public void setPreferredLocation(Point preferredLocation)
	{
		this.preferredLocation = preferredLocation;
	}

	@Override
	public void setPreferredSize(Dimension preferredSize)
	{
		this.preferredSize = preferredSize;
	}

	private static final class Cell
	{
		private final String text;
		private final Color color;

		private Cell(String text, Color color)
		{
			this.text = text;
			this.color = color;
		}

		private static Cell parse(String raw)
		{
			if (raw == null)
			{
				return new Cell("", null);
			}

			final Matcher matcher = LEADING_COLOR_TAG.matcher(raw);
			if (matcher.matches())
			{
				try
				{
					return new Cell(matcher.group(2), Color.decode("#" + matcher.group(1)));
				}
				catch (NumberFormatException e)
				{
					return new Cell(raw, null);
				}
			}

			return new Cell(raw, null);
		}
	}
}
