/**
 * Copyright 2020 Alexander Herzog
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mathtools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.binary.XSSFBSheetHandler.SheetContentsHandler;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Diese Klasse lie�t eine Exceltabelle in eine {@link Table} oder eine {@link MultiTable}
 * im Streaming-Modus (d.h. mit geringem Speicherverbrauch) ein.
 * @author Alexander Herzog
 * @see Table
 * @see MultiTable
 * @version 1.1
 */
public class TableXLSXReader {
	private final File file;
	private final MultiTable multiTable;

	/**
	 * Konstruktor der Klasse
	 * @param file	Zu lesende Tabellendatei
	 */
	public TableXLSXReader(final File file) {
		this.file=file;
		multiTable=new MultiTable();
	}

	/**
	 * Liefert im Falle der Verarbeitung durch {@link TableXLSXReader#process()} die Ergebnisarbeitsmappe.
	 * @return	Geladene Arbeitsmappe
	 */
	public MultiTable getTable() {
		return multiTable;
	}

	private boolean processSheet(final String name, final StylesTable styles, final ReadOnlySharedStringsTable strings, InputStream stream, Table useTable) {
		final DataFormatter formatter=new DataFormatter();
		final InputSource sheetSource=new InputSource(stream);
		final Table table=(useTable==null)?new Table():useTable;

		try {
			final XMLReader sheetParser=XMLHelper.newXMLReader();
			final ContentHandler handler=new XSSFSheetXMLHandler(styles,null,strings,new SheetToTable(table),formatter,false);
			sheetParser.setContentHandler(handler);
			sheetParser.parse(sheetSource);
		} catch(ParserConfigurationException | SAXException | IOException e) {
			return false;
		}

		if (useTable==null) multiTable.add(name,table);
		return true;
	}

	private boolean processOPCPacakge(final OPCPackage xlsxPackage, final Table table) {
		try {
			final ReadOnlySharedStringsTable strings=new ReadOnlySharedStringsTable(xlsxPackage);
			final XSSFReader xssfReader=new XSSFReader(xlsxPackage);
			final StylesTable styles=xssfReader.getStylesTable();
			final XSSFReader.SheetIterator iter=(XSSFReader.SheetIterator) xssfReader.getSheetsData();

			while (iter.hasNext()) try (InputStream stream=iter.next()) {
				if (!processSheet(iter.getSheetName(),styles,strings,stream,table)) return false;
				if (table!=null) return true;
			}
			return true;

		} catch (IOException | SAXException | OpenXML4JException e1) {
			return false;
		}
	}

	/**
	 * Verarbeitet die gesamte Arbeitsmappe.
	 * Das Ergebnis kann im Erfolgsfall �ber die Funktion {@link TableXLSXReader#getTable()} abgerufen werden.
	 * @return	Liefert im Erfolgsfall <code>true</code>.
	 */
	public boolean process() {
		if (file==null || !file.isFile()) return false;

		try (OPCPackage xlsxPackage=OPCPackage.open(file,PackageAccess.READ)) {
			return processOPCPacakge(xlsxPackage,null);
		} catch (InvalidFormatException | IOException e) {
			return false;
		}
	}

	/**
	 * L�dt nur die erste Tabelle der Datei.
	 * @param table	{@link Table}-Objekt in das die Daten geladen werden sollen.
	 * @return	Liefert im Erfolgsfall <code>true</code>.
	 */
	public boolean processFirstTableOnly(final Table table) {
		if (file==null || !file.isFile() || table==null) return false;

		try (OPCPackage xlsxPackage=OPCPackage.open(file,PackageAccess.READ)) {
			return processOPCPacakge(xlsxPackage,table);
		} catch (InvalidFormatException | IOException e) {
			return false;
		}
	}

	private static class SheetToTable implements SheetContentsHandler {
		private final Table table;
		private List<String> row;

		private SheetToTable(final Table table) {
			this.table=table;
		}

		private void outputMissingRows(int number) {
			for (int i=0;i<number;i++) table.addLine(new ArrayList<>());
		}

		@Override
		public void startRow(int rowNum) {
			endRow(-1);
			outputMissingRows(rowNum-table.getSize(0));
			row=new ArrayList<>();
		}

		@Override
		public void endRow(int rowNum) {
			if (row!=null) table.addLine(row);
			row=null;
		}

		@Override
		public void cell(String cellReference, String formattedValue, XSSFComment comment) {
			if (row==null) return;

			if (cellReference==null) cellReference=new CellAddress(table.getSize(0),row.size()).formatAsString();

			int thisCol=(new CellReference(cellReference)).getCol();
			int missedCols=thisCol-row.size();
			for (int i=0;i<missedCols;i++) row.add("");

			row.add(formattedValue);
		}

		@Override
		public void hyperlinkCell(String cellReference, String formattedValue, String arg2, String arg3, XSSFComment comment) {
			cell(cellReference,formattedValue,comment);
		}
	}
}
