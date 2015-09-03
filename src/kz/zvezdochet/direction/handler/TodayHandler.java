package kz.zvezdochet.direction.handler;

import java.util.Date;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.DatePart;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

/**
 * Обработчик расчёта транзитов на сегодня
 * @author nataly
 *
 */
public class TodayHandler extends DateCalcHandler {
	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт транзитов на сегодня", false);
			DatePart datePart = (DatePart)activePart.getObject();
			if (!datePart.check(0)) return;
			Event person = datePart.getPerson();

			updateStatus("Расчёт транзитов на указанную дату", false);
			Date seldate = datePart.getDate();
			Event event = new Event();
			event.setBirth(seldate);
			event.calc(false);

			makeTransits(person, event);
		    datePart.setData(aged);
			updateStatus("Таблица транзитов сформирована", false);

			datePart.setEvent(event);
			datePart.onCalc(0);
			updateStatus("Космограмма транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}