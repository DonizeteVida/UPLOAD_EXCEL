package jaguar.donizete.mBeans;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.formula.CollaboratingWorkbooksEnvironment.WorkbookNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.model.UploadedFile;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseReference.CompletionListener;
import com.google.firebase.database.FirebaseDatabase;

@ManagedBean
@SessionScoped
public class IndexMBean {
	private String nome = "Importar planilha excel";
	private UploadedFile planilha;
	private File planilhaFile;
	DatabaseReference database;
	Integer incre = 0;

	public IndexMBean() {
		try {
			initBanco();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println(e.toString());
		}

		database = FirebaseDatabase.getInstance().getReference();

	}

	private void salvarAlgumDado(String s, String c) {
		incre++;

		database.child(c).setValue(s, new CompletionListener() {
			@Override
			public void onComplete(DatabaseError error, DatabaseReference ref) {
				// TODO Auto-generated method stub
				System.out.println("Sucesso !");
			}
		});
	}

	private void initBanco() throws IOException {
		String caminho = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/WEB-INF/classes/json");

		System.out.println(caminho);

		FileInputStream serviceAccount = new FileInputStream(caminho + "/key.json");
		serviceAccount.toString();

		FirebaseOptions options = new FirebaseOptions.Builder()
				.setCredentials(GoogleCredentials.fromStream(serviceAccount))
				.setDatabaseUrl("https://graficosdados.firebaseio.com/").build();

		FirebaseApp.initializeApp(options);
	}

	public void uploadPlanilha(FileUploadEvent fileUploadEvent) {
		setPlanilha(fileUploadEvent.getFile());
	}

	public UploadedFile getPlanilha() {
		return planilha;
	}

	public void setPlanilha(UploadedFile planilha) {
		this.planilha = planilha;
		String caminhoGravar = FacesContext.getCurrentInstance().getExternalContext().getRealPath("Excel arquivado");

		File file = new File(caminhoGravar, planilha.getFileName());

		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		try {
			OutputStream outputStream = new FileOutputStream(file);
			outputStream.write(planilha.getContents());
			outputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		planilhaFile = file;
		System.out.println(file.getAbsolutePath());
		new Thread(

				new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						try {
							salvar();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}).start();

	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	private void salvar() throws IOException {
		InputStream inputStream = new FileInputStream(planilhaFile);
		XSSFWorkbook hssfWorkbook = new XSSFWorkbook(inputStream);
		XSSFSheet hssfSheet = hssfWorkbook.getSheetAt(0);

		XSSFFormulaEvaluator formulaEvaluator = hssfWorkbook.getCreationHelper().createFormulaEvaluator();
		formulaEvaluator.setIgnoreMissingWorkbooks(true);

		XSSFRow row;
		XSSFCell cell;

		Iterator rows = hssfSheet.rowIterator();

		while (rows.hasNext()) {
			row = (XSSFRow) rows.next();

			Iterator cells = row.cellIterator();

			while (cells.hasNext()) {

				cell = (XSSFCell) cells.next();
				CellValue cellValue;

				cellValue = formulaEvaluator.evaluate(cell);
				CellReference reference = new CellReference(row.getRowNum(), cell.getColumnIndex());
				if (cellValue != null) {
					salvarAlgumDado(cellValue.formatAsString(),
							reference.getCellRefParts()[1] + reference.getCellRefParts()[2]);
				}

			}
		}
	}

}
