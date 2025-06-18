package dk.digitalidentity.os2vikar.service.xls;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import dk.digitalidentity.os2vikar.dao.model.Statistic;

public class StatisticXlsView extends AbstractXlsxView {
	private int rowCount = 1;

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {

		@SuppressWarnings("unchecked")
		List<Statistic> statistics = (List<Statistic>) model.get("statistics");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet("Vikarstatistik");

		// create header row
		createHeader(workbook, sheet);

		for (Statistic statistic : statistics) {
			Row row = sheet.createRow(rowCount++);

			row.createCell(0).setCellValue(statistic.getWorkplaceId());
			row.createCell(1).setCellValue(statistic.getSubstituteName());
			row.createCell(2).setCellValue(statistic.getSubstituteUserId());
			row.createCell(3).setCellValue(statistic.getOrgunitName());
			row.createCell(4).setCellValue(statistic.getOrgunitUuid());
			row.createCell(5).setCellValue(statistic.getStartDate().toString());
			row.createCell(6).setCellValue(statistic.getStopDate().toString());
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet) {
		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, "ID", headerStyle);
		createCell(header, 1, "Vikar", headerStyle);
		createCell(header, 2, "Brugernavn", headerStyle);
		createCell(header, 3, "Enhed", headerStyle);
		createCell(header, 4, "Enhed UUID", headerStyle);
		createCell(header, 5, "Start dato", headerStyle);
		createCell(header, 6, "Stop dato", headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
