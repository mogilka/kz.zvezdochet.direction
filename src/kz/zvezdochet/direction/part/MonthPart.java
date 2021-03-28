package kz.zvezdochet.direction.part;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.ArrayLabelProvider;
import kz.zvezdochet.core.ui.comparator.TableSortListenerFactory;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.listener.ListSelectionListener;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.part.Messages;
import kz.zvezdochet.provider.PlaceProposalProvider;
import kz.zvezdochet.provider.PlaceProposalProvider.PlaceContentProposal;

/**
 * Сводная таблица транзитов за месяц
 * @author Natalie Didenko
 */
public class MonthPart extends ModelListView implements ICalculable {
	@Inject
	public MonthPart() {}

	private Event person;

	private DateTime dt;
	private DateTime dt2;
	private Text txPlace;
	private Text txLatitude;
	private Text txLongitude;
	private Text txZone;
	private Text txGreenwich;

	public void setPerson(Event person) {
		this.person = person;
		initPlace(person.getCurrentPlace());
	}

	@PostConstruct @Override
	public View create(Composite parent) {
		return super.create(parent);
	}

	@Override
	public boolean check(int mode) throws Exception {
		if (txZone.getText().equals(""))
			txZone.setText("0.0");

		Date date = getInitialDate();
		Date date2 = getFinalDate();
		if (!DateUtil.isDateRangeValid(date, date2)) {
			DialogUtil.alertWarning("Укажите правильный период");
			return false;
		}
		if (null == person) {
			DialogUtil.alertWarning("Событие не задано");
			return false;
		}
		return true;
	}

	@Override
	public void initFilter(Composite parent) {
		grFilter = new Group(parent, SWT.NONE);
		grFilter.setText("Период");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Начало");
		dt = new DateTime(grFilter, SWT.DROP_DOWN);

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Конец");
		dt2 = new DateTime(grFilter, SWT.DROP_DOWN);

		Group secPlace = new Group(grFilter, SWT.NONE);
		secPlace.setText(Messages.getString("PersonView.Place")); //$NON-NLS-1$
		txPlace = new Text(secPlace, SWT.BORDER);
		new InfoDecoration(txPlace, SWT.TOP | SWT.LEFT);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Latitude")); //$NON-NLS-1$
		txLatitude = new Text(secPlace, SWT.BORDER);
		txLatitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Longitude")); //$NON-NLS-1$
		txLongitude = new Text(secPlace, SWT.BORDER);
		txLongitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Greenwith")); //$NON-NLS-1$
		txGreenwich = new Text(secPlace, SWT.BORDER);
		txGreenwich.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Zone")); //$NON-NLS-1$
		txZone = new Text(secPlace, SWT.BORDER);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(secPlace);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, false).applyTo(secPlace);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
//			grab(true, false).applyTo(dt);
//		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
//			grab(true, false).applyTo(dt2);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			span(4, 1).grab(true, false).applyTo(txPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLatitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLongitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txZone);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txGreenwich);
	}

	/**
	 * Поиск персоны, для которой рассчитывается транзит
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	@Override
	public void reset() {
//		dt.setNullText("");
//		dt2.setNullText("");
//		txPlace.setText(""); //$NON-NLS-1$
//		txLatitude.setText(""); //$NON-NLS-1$
//		txLongitude.setText(""); //$NON-NLS-1$
//		txZone.setText(""); //$NON-NLS-1$
//		txGreenwich.setText(""); //$NON-NLS-1$
		super.reset();
		tableViewer2.getTable().removeAll();
	}

	public Date getInitialDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, dt.getDay());
		calendar.set(Calendar.MONTH, dt.getMonth());
		calendar.set(Calendar.YEAR, dt.getYear());
		return calendar.getTime();
	}

	public Date getFinalDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.DAY_OF_MONTH, dt2.getDay());
		calendar.set(Calendar.MONTH, dt2.getMonth());
		calendar.set(Calendar.YEAR, dt2.getYear());
		return calendar.getTime();
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		String[] columns = new String[32];
		columns[0] = "";
		for (int i = 1; i < 32; i++)
			columns[i] = String.valueOf(i);
		return columns;
	}

	/**
	 * Инициализация представления местности события
	 * @param place местность
	 */
	private void initPlace(Place place) {
		if (null == place) return;
		txPlace.setText(place.getName());
		txLatitude.setText(CalcUtil.formatNumber("###.##", place.getLatitude())); //$NON-NLS-1$
		txLongitude.setText(CalcUtil.formatNumber("###.##", place.getLongitude())); //$NON-NLS-1$
		txGreenwich.setText(CalcUtil.formatNumber("###.##", place.getGreenwich())); //$NON-NLS-1$
		txZone.setText(String.valueOf(place.getZone()));
	}

	/**
	 * Инициализация местностей
	 */
	private void setPlaces() {
	    PlaceProposalProvider proposalProvider = new PlaceProposalProvider();
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txPlace, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Place place = (Place)((PlaceContentProposal)proposal).getObject();
				if (place != null) {
					person.setCurrentPlace(place);
					initPlace(place);
				}
			}
		});
	}

	@SuppressWarnings("unlikely-arg-type")
	protected void initControls() throws DataAccessException {
		super.initControls();
		if (person != null)
			initPlace(person.getCurrentPlace());
		setPlaces();

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.set(Calendar.DAY_OF_MONTH, 1);
		int month = calendar.get(Calendar.MONTH);
		dt.setDate(calendar.get(Calendar.YEAR), month, calendar.get(Calendar.DAY_OF_MONTH));

		int date = 31;
		if (1 == month)
			date = calendar.isLeapYear(calendar.get(Calendar.YEAR)) ? 28 : 29;
		else if (Arrays.asList(new int[] {4, 6, 9, 11}).contains(month))
			date = 30;
		dt2.setDate(calendar.get(Calendar.YEAR), month, date);

		tableViewer2.setContentProvider(new ArrayContentProvider());
		tableViewer2.setLabelProvider(getLabelProvider());
		
		ListSelectionListener listener = getSelectionListener();
		tableViewer2.addSelectionChangedListener(listener);
		tableViewer2.addDoubleClickListener(listener);
	}

	public Place getPlace() {
		Place place = person.getCurrentPlace();
		return (null == place) ? new Place().getDefault() : person.getCurrentPlace();
	}

	public double getZone() {
		return Double.parseDouble(txZone.getText());
	}

	@Override
	protected IBaseLabelProvider getLabelProvider() {
		return new ArrayLabelProvider();
	}

	@Override
	public void onCalc(Object obj) {
		// TODO Auto-generated method stub
		
	}

	private TableViewer tableViewer2;

	@Override
	protected void initGroup() {
		tableViewer2 = new TableViewer(group, SWT.BORDER | SWT.FULL_SELECTION);
		Table table = tableViewer2.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		addColumns2();

		GridLayoutFactory.swtDefaults().applyTo(group);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
//		GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).
//			hint(514, 514).span(3, 1).grab(true, false).applyTo(cmpCosmogram);
	}

	private void addColumns2() {
		removeColumns2();
		String[] columns = initTableColumns();
		if (columns != null)
			for (String column : columns) {
				TableColumn tableColumn = new TableColumn(tableViewer2.getTable(), SWT.NONE);
				tableColumn.setText(column);		
				tableColumn.addListener(SWT.Selection, TableSortListenerFactory.getListener(
					TableSortListenerFactory.STRING_COMPARATOR));
			}
	}

	private void removeColumns2() {
		for (TableColumn column : tableViewer2.getTable().getColumns())
			column.dispose();
	}

	/**
	 * Инициализация второй таблицы значениями из БД
	 */
	protected void initTable2() {
		try {
			showBusy(true);
			Table table = tableViewer2.getTable();
			if (null == data2)
				table.removeAll();
			else {
				tableViewer2.setInput(data2);
				for (int i = 0; i < table.getColumnCount(); i++)
					table.getColumn(i).pack();
			}
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}

	/**
	 * Массив данных второй таблицы
	 */
	protected Object data2;

	/**
	 * Инициализация содержимого второй таблицы
	 * @param data массив данных
	 */
	public void setData2(Object data) {
		try {
			showBusy(true);
			this.data2 = data;
			initTable2();	
		} finally {
			showBusy(false);
		}
	}

	@Override
	protected void arrange(Composite parent) {
		super.arrange(parent);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tableViewer2.getTable());
	}
}
