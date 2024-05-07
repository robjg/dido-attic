package org.oddjob.dido.poi.layouts;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.oddjob.dido.DataException;
import org.oddjob.dido.DataIn;
import org.oddjob.dido.DataOut;
import org.oddjob.dido.DataReader;
import org.oddjob.dido.DataWriter;
import org.oddjob.dido.Layout;
import org.oddjob.dido.ValueNode;
import org.oddjob.dido.bio.BeanBindingBean;
import org.oddjob.dido.field.Field;
import org.oddjob.dido.layout.LayoutNode;
import org.oddjob.dido.layout.NullReader;
import org.oddjob.dido.morph.MorphDefinition;
import org.oddjob.dido.morph.MorphDefinitionBuilder;
import org.oddjob.dido.morph.MorphProvider;
import org.oddjob.dido.morph.Morphable;
import org.oddjob.dido.poi.RowsIn;
import org.oddjob.dido.poi.RowsOut;
import org.oddjob.dido.poi.SheetIn;
import org.oddjob.dido.poi.SheetOut;
import org.oddjob.dido.poi.data.PoiRowsIn;
import org.oddjob.dido.poi.data.PoiRowsOut;
import org.oddjob.dido.poi.utils.CellTypeFactory;

/**
 * @oddjob.description Define an area in a spreadsheet sheet for reading
 * and writing rows to.
 * <p>
 * This layout will generally be nested in a {@link DataSheet} but can
 * be nested in a {@link DataBook} for convenience. In this case the
 * next sheet in the book is used.
 * <p>
 * This layout is a typical candidate for attaching an {@link BeanBindingBean}
 * to.
 * <p>
 * This layout is Morphable which means that a binding can ask it to 
 * generate it's children If no child nodes (cells) are already defined.
 * 
 * @author rob
 *
 */
public class DataRows extends LayoutNode 
implements Morphable, MorphProvider {
	
	private static final Logger logger = Logger.getLogger(DataRows.class);
	
	/**
	 * @oddjob.property
	 * @oddjob.description The starting row in the sheet.
	 * @oddjob.required No. Defaults to 1.
	 */
	private int firstRow = 1;
	
	/**
	 * @oddjob.property
	 * @oddjob.description The starting column in the sheet.
	 * @oddjob.required No. Defaults to 1.
	 */
	private int firstColumn = 1;
	
	/**
	 * @oddjob.property
	 * @oddjob.description The last row in the sheet written to by this 
	 * layout.
	 * @oddjob.required Read Only.
	 */
	private int lastRow;
	
	/**
	 * @oddjob.property
	 * @oddjob.description The last column in the sheet written to by this 
	 * layout..
	 * @oddjob.required No. Defaults to 1.
	 */
	private int lastColumn;
	
	/**
	 * @oddjob.property
	 * @oddjob.description Should a header row be written (true/false).
	 * @oddjob.required No. Defaults to false.
	 */
	private boolean withHeadings;

	/**
	 * @oddjob.property
	 * @oddjob.description The style name used in the header row.
	 * @oddjob.required No. A default header style will be used.
	 */
	private String headingsStyle;
	
	/**
	 * @oddjob.property
	 * @oddjob.description If true then automatically set the width of the column to the
	 * widest column value when values have finished being written.
	 * @oddjob.required No. Defaults to false.
	 */
	private boolean autoWidth;
	
	/**
	 * @oddjob.property
	 * @oddjob.description If true then automatically set an auto filter on
	 * the column.
	 * @oddjob.required No. Defaults to false.
	 */
	private boolean autoFilter;
	
	/**
	 * @oddjob.property
	 * @oddjob.description Set when reading if there is a header row to
	 * read headings from. Used to derive a binding type if one is required
	 * and there an no child layouts to derive it from.
	 * @oddjob.required No. Read only.
	 */
	private String[] headings;
	
	/**
	 * @oddjob.property of
	 * @oddjob.description The child layouts for the rows. These define
	 * the columns created. Typically they will be one of {@link TextCell},
	 * {@link NumericCell}, {@link DateCell}, {@link BooleanCell}, 
	 * or {@link NumericFormulaCell}.
	 * @oddjob.required No, they can be generated by a binding.
	 * 
	 * @param index 0 based index.
	 * @param child The child, null will remove the child for the given index.
	 */
	public void setOf(int index, Layout child) {
		addOrRemoveChild(index, child);
	}
	
	@Override
	public Runnable morphInto(MorphDefinition morphicness) {
		
		if (childLayouts().size() > 0) {
			logger.debug("[" + this + "] has children. Morphicness ignored.");
			
			return new Runnable() {
				@Override
				public void run() {
				}
			};
		}
		
		String[] properties = morphicness.getNames();
		
		int i = 0;
		for (String property : properties) {
			
			Class<?> propertyType = morphicness.typeOf(property);
			
			DataCell<?> cell = new CellTypeFactory<DataCell<?>>() {
				@Override
				protected DataCell<?> createBoolean() {
					return new BooleanCell();
				}
				@Override
				protected DataCell<?> createDate() {
					return new DateCell();
				}
				@Override
				protected DataCell<?> createNumeric() {
					return new NumericCell();
				}
				@Override
				protected DataCell<?> createText() {
					return new TextCell();
				}
			}.createFor(propertyType);
			
			cell.setName(property);
			cell.setLabel(morphicness.labelFor(property));
			
			logger.debug("[" + this + "] adding morphicness [" + cell + "]");
			
			setOf(i++, cell);
		}
		
		return new Runnable() {
			
			@Override
			public void run() {
				childLayouts().clear();
			}
		};
	}
			
	@Override
	public MorphDefinition morphOf() {
	
		Iterable<Layout> childLayouts = childLayouts();
		
		if (childLayouts == null) {
			return null;
		}
		
		MorphDefinitionBuilder morphBuilder = new MorphDefinitionBuilder();
		
		for (Layout child : childLayouts) {
			String name = child.getName();
			if (name == null) {
				continue;
			}
			
			if (!(child instanceof ValueNode)) {
				continue;
			}
			
			Class<?> type = ((ValueNode<?>) child).getType();
			
			String label = null;
			if (child instanceof Field) {
				label = ((Field) child).getLabel();
			}

			morphBuilder.add(name, label, type);
		}
		
		return morphBuilder.build();
	}
	
	class MainReader implements DataReader {
	
		private final RowsIn rowsIn; 
		
		private DataReader nextReader;
		
		public MainReader(RowsIn rowsIn) {
			this.rowsIn = rowsIn;
		}
		
		@Override
		public Object read() throws DataException {
			
			if (nextReader != null) {
				Object value = nextReader.read();
				
				if (value != null) {
					
					lastRow = rowsIn.getLastRow();
					lastColumn = rowsIn.getLastColumn();
					
					return value;
				}
			}
			
			if (!rowsIn.nextRow()) {
				return null;
			}

			logger.debug("[" + DataRows.this + "] reading row " + 
						rowsIn.getLastRow());

			nextReader = nextReaderFor(rowsIn);
			
			return read();
		}
		
		@Override
		public void close() throws DataException {
			if (nextReader != null) {
				nextReader.close();
				nextReader = null;
			}
			
			logger.debug("[" + DataRows.this +  
					"] closed reader at row, column [" + lastRow + 
					", " + lastColumn + "]");
		}
	}
	
	@Override
	public DataReader readerFor(DataIn dataIn) throws DataException {
		
		SheetIn sheetIn = dataIn.provideDataIn(SheetIn.class);
		
		RowsIn rowsIn = new PoiRowsIn(sheetIn, firstRow, firstColumn);
		
		if (withHeadings) {
			
			this.headings = rowsIn.headerRow();
			if (headings == null) {
				logger.info("[" + this + "] No rows to provide reader from.");
				return new NullReader();
			}
			else {
				logger.info("[" + this + "] Read headings " + 
						Arrays.toString(this.headings));
			}
		}
		else {
			logger.debug("[" + this + "] Providing reader with no headings.");
		}
		
		return new MainReader(rowsIn);
	}

	class MainWriter implements DataWriter {
		
		private final RowsOut rowsOut;
		
		private DataWriter nextWriter;
		
		public MainWriter(RowsOut dout) {
			this.rowsOut = dout;
		}
		
		@Override
		public boolean write(Object object) throws DataException {
			
			if (nextWriter == null) {

				rowsOut.nextRow();
				
				nextWriter = nextWriterFor(rowsOut);
			}
			
			logger.trace("[" + DataRows.this + "] writing row " + 
					rowsOut.getLastRow());
			
			if (nextWriter.write(object)) {
				return true;
			}
			
			lastRow = rowsOut.getLastRow();
			lastColumn = rowsOut.getLastColumn();

			nextWriter.close();
			nextWriter = null;

			// Want to be kept by parent and given more data.
			return true;
		}
		
		@Override
		public void close() throws DataException {
			
			if (nextWriter != null) {
				nextWriter.close();
				nextWriter = null;
			}
				
			if (autoFilter) {
				rowsOut.autoFilter();
			}
			
			if (autoWidth) {
				rowsOut.autoWidth();
			}
			
			logger.debug("[" + DataRows.this +  
					"] closed writer at row, column [" + lastRow + 
					", " + lastColumn + "]");
		}
	}
	
	@Override
	public DataWriter writerFor(DataOut dataOut) throws DataException {

		final SheetOut sheetOut = dataOut.provideDataOut(SheetOut.class);
		
		logger.debug("Creating writer for [" + sheetOut + "]");

		PoiRowsOut rowsOut = new PoiRowsOut(sheetOut, firstRow, firstColumn);
		
		if (withHeadings) {
			rowsOut.headerRow(headingsStyle);
			
			logger.debug("[" + this + "] initialsed at [" + 
					firstRow + ", " + firstColumn + "]");
		}
		
		return new MainWriter(rowsOut) {
			
			@Override
			public void close() throws DataException {
				super.close();
				sheetOut.close();
			}
		};
	}

	@Override
	public void reset() {
		super.reset();
		
		lastRow = 0;
		lastColumn = 0;
	}
	
	public boolean isWithHeadings() {
		return withHeadings;
	}

	public void setWithHeadings(boolean withHeading) {
		this.withHeadings = withHeading;
	}

	public boolean isAutoWidth() {
		return autoWidth;
	}

	public void setAutoWidth(boolean autoWidth) {
		this.autoWidth = autoWidth;
	}

	public String getHeadingsStyle() {
		return headingsStyle;
	}

	public void setHeadingsStyle(String headingsStyle) {
		this.headingsStyle = headingsStyle;
	}
	
	public int getFirstRow() {
		return firstRow;
	}

	public void setFirstRow(int startRow) {
		this.firstRow = startRow;
	}

	public int getFirstColumn() {
		return firstColumn;
	}

	public void setFirstColumn(int startColumn) {
		this.firstColumn = startColumn;
	}

	public boolean isAutoFilter() {
		return autoFilter;
	}

	public void setAutoFilter(boolean autoFilter) {
		this.autoFilter = autoFilter;
	}

	public int getLastRow() {
		return lastRow;
	}

	public int getLastColumn() {
		return lastColumn;
	}
	
	public String[] getHeadings() {
		return headings;
	}
	
	@Override
	public String toString() {
		String name = getName();
		return getClass().getSimpleName() + 
				(name == null ? "" : " " + name);
	}

}
