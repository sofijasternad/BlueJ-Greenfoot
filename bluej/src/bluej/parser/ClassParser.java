// $ANTLR 2.7.2: "java.g" -> "ClassParser.java"$

    package bluej.parser;

import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

    import bluej.parser.symtab.SymbolTable;
    import bluej.parser.symtab.JavaVector;
    import bluej.parser.symtab.DummyClass;
    import bluej.parser.symtab.ClassInfo;
    import bluej.parser.symtab.Selection;

    import antlr.*;

    import java.util.Vector;
    import java.io.*;

    class JavaBitSet extends java.util.BitSet
    {
    }


public class ClassParser extends antlr.LLkParser       implements JavaTokenTypes
 {

    // these static variables are used to tell what kind of compound
    // statement is being parsed (see the compoundStatement rule
    static final int BODY          = 1;
    static final int CLASS_INIT    = 2;
    static final int INSTANCE_INIT = 3;
    static final int NEW_SCOPE     = 4;

    // these static variables are used as indices into a BitSet which
    // shows the modifiers of a class
    static final int MOD_PRIVATE	= 0;
    static final int MOD_PUBLIC	= 1;
    static final int MOD_PROTECTED	= 2;
    static final int MOD_STATIC	= 3;
    static final int MOD_ABSTRACT	= 4;

    // We need a symbol table to track definitions
    private SymbolTable symbolTable;
    private TokenStreamHiddenTokenFilter filter;
    private ClassInfo info;

    public static ClassInfo parse(String filename)
        throws Exception
    {
    	return parse(filename, null);
    }

    public static ClassInfo parse(File file)
        throws Exception
    {
    	return parse(file, null);
    }

    // the main entry point to parse a file
    public static ClassInfo parse(String filename, Vector classes)
        throws Exception
    {
    	return parse(new File(filename), classes);
    }

    public static ClassInfo parse(File file, Vector classes)
    	throws Exception
    {
	// create a new symbol table
	SymbolTable symbolTable = new SymbolTable();
        ClassInfo info = new ClassInfo();

	doFile(file, symbolTable, info); // parse it

	// resolve the types of all symbols in the symbol table
	//  -- we don't need this for BlueJ
	// symbolTable.resolveTypes();

	// add existing classes to the symbol table
	if(classes != null)
		symbolTable.addClasses(classes);

	symbolTable.getInfo(info);

	return info;
    }


    // This method decides what action to take based on the type of
    //   file we are looking at
    private static void doFile(File f, SymbolTable symbolTable, ClassInfo info)
	throws Exception
    {
        // If this is a directory, walk each file/dir in that directory
        if (f.isDirectory()) {
		throw new Exception("Attempt to parse directory");
        }

        // otherwise, if this is a java file, parse it!
        else if (f.getName().endsWith(".java")) {
            symbolTable.setFile(f);
            parseFile(new BufferedReader(new FileReader(f)), symbolTable, info);
        }
    }

    // Here's where we do the real work...
    private static void parseFile(Reader r,
                                 SymbolTable symbolTable, ClassInfo info)
	throws Exception
    {
	// Create a scanner that reads from the input stream passed to us
	JavaLexer lexer = new JavaLexer(r);

	// Tell the scanner to create tokens of class JavaToken
	lexer.setTokenObjectClass("bluej.parser.JavaToken");

	TokenStreamHiddenTokenFilter filter = new TokenStreamHiddenTokenFilter(lexer);

	// Tell the lexer to redirect all multiline comments to our
	// hidden stream
	filter.hide(ClassParser.ML_COMMENT);

	// Create a parser that reads from the scanner
	ClassParser parser = new ClassParser(filter);

	// Tell the parser to use the symbol table passed to us
	parser.setSymbolTable(symbolTable);
	parser.setClassInfo(info);
	parser.setFilter(filter);

	// start parsing at the compilationUnit rule
	parser.compilationUnit();
	
	//close the reader to allow class name changes in editor
	r.close();
    }

    // Tell the parser which symbol table to use
    public void setSymbolTable(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public void setClassInfo(ClassInfo info) {
    	this.info = info;
    }

    public void setFilter(TokenStreamHiddenTokenFilter filter) {
        this.filter = filter;
    }


    // redefined from antlr.LLkParser to supress error messages
    public void reportError(RecognitionException ex) {
        // do nothing
    }

	public JavaToken findAttachedComment(JavaToken startToken)
	{
		CommonHiddenStreamToken ctok = null;
		if (startToken != null) {
			ctok = filter.getHiddenBefore(startToken);

			if(ctok != null) {
				// if the last line of the comment is more than
				// two away from the start of the method/class
				// then we can't really say that its attached.
				// I believe it is part of the javadoc spec that
				// says that comments and their method have to
				// be right next to each other but I could be
				// wrong
				/* disabled by ajp. AS of antlr 2.7.1, getLine()
				   refers to the start line of the comment, not
				   the end line so the following logic doesn't work.
				   I don't think its important enough to bother
				   reimplementing */
				/* if (ctok.getLine() < startToken.getLine()-2)
					ctok = null; */
			}
		}

		return (JavaToken)ctok;
	}

    //------------------------------------------------------------------------
    // Symboltable adapter methods
    // The following methods are provided to give a single set of entry
    //   calls into the symbol table.  This makes it easy to add debugging
    //   code that will track all calls to symbolTable.popScope, for instance
    // These are direct pass-through calls to the symbolTable, but a
    //   few have special function.
    //------------------------------------------------------------------------

    public void popScope()                 {//System.out.println("pop");
                                            symbolTable.popScope();}
    public void endFile()                  {symbolTable.popAllScopes();}
    public void defineBlock(JavaToken tok) {//System.out.println("entering block" + tok);
                                            symbolTable.defineBlock(tok);}
    public void definePackage(JavaToken t) {symbolTable.definePackage(t);}
    public void defineLabel(JavaToken t)   {symbolTable.defineLabel(t);}
    public void useDefaultPackage()        {symbolTable.useDefaultPackage();}
    public void reference(JavaToken t)     {symbolTable.reference(t);}
    public void setNearestClassScope()     {symbolTable.setNearestClassScope();}

    public void endMethodHead(JavaVector exceptions) {
        symbolTable.endMethodHead(exceptions);
    }

    public DummyClass dummyClass(JavaToken theClass) {
        return symbolTable.getDummyClass(theClass);
    }


    public void defineClass(JavaToken theClass,
                            JavaToken superClass,
                            JavaVector interfaces,
			    boolean isAbstract,
			    boolean isPublic,
			    JavaToken comment,
			    Selection extendsInsert, Selection implementsInsert,
			    Selection extendsReplace, Selection superReplace,
			    Vector interfaceSelections)
    {
        symbolTable.defineClass(theClass, superClass, interfaces, isAbstract, isPublic,
        			comment, extendsInsert, implementsInsert,
        			extendsReplace, superReplace, interfaceSelections);
    }

    public void defineInterface(JavaToken theInterface,
                                JavaVector superInterfaces,
                                boolean isPublic,
                                JavaToken comment,
                                Selection extendsInsert,
                                Vector superInterfaceSelections)
    {
        symbolTable.defineInterface(theInterface, superInterfaces, isPublic, comment,
                                    extendsInsert, superInterfaceSelections);
    }

    public void defineVar(JavaToken theVariable, JavaToken type, JavaToken comment) {
        symbolTable.defineVar(theVariable, type, comment);
    }


    public void defineMethod(JavaToken theMethod, JavaToken type, JavaToken comment) {
        //System.out.println("entering method" + theMethod);
        symbolTable.defineMethod(theMethod, type, comment);
    }

    public void addImport(JavaToken id, String className, String packageName) {
        symbolTable.addImport(id, className, packageName);
    }

    // create a selection which consists of the location just after the token passed
    // in
    public Selection selectionAfterToken(JavaToken id)
    {
	return new Selection(id.getFile(), id.getLine(),
                              id.getColumn() + id.getText().length());
    }

protected ClassParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public ClassParser(TokenBuffer tokenBuf) {
  this(tokenBuf,2);
}

protected ClassParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public ClassParser(TokenStream lexer) {
  this(lexer,2);
}

public ClassParser(ParserSharedInputState state) {
  super(state,2);
  tokenNames = _tokenNames;
}

	public final void compilationUnit() throws RecognitionException, TokenStreamException {
		
		
		{
		switch ( LA(1)) {
		case LITERAL_package:
		{
			packageDefinition();
			break;
		}
		case EOF:
		case FINAL:
		case ABSTRACT:
		case STRICTFP:
		case SEMI:
		case LITERAL_import:
		case LITERAL_private:
		case LITERAL_public:
		case LITERAL_protected:
		case LITERAL_static:
		case LITERAL_transient:
		case LITERAL_native:
		case LITERAL_threadsafe:
		case LITERAL_synchronized:
		case LITERAL_volatile:
		case LITERAL_class:
		case LITERAL_interface:
		{
			if ( inputState.guessing==0 ) {
				useDefaultPackage();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop4:
		do {
			if ((LA(1)==LITERAL_import)) {
				importDefinition();
			}
			else {
				break _loop4;
			}
			
		} while (true);
		}
		{
		_loop6:
		do {
			if ((_tokenSet_0.member(LA(1)))) {
				typeDefinition();
			}
			else {
				break _loop6;
			}
			
		} while (true);
		}
		match(Token.EOF_TYPE);
		if ( inputState.guessing==0 ) {
			endFile();
		}
	}
	
	public final void packageDefinition() throws RecognitionException, TokenStreamException {
		
		Token  pkg = null;
		Token  sem = null;
		JavaToken id;
		
		try {      // for error handling
			pkg = LT(1);
			match(LITERAL_package);
			id=identifier();
			sem = LT(1);
			match(SEMI);
			if ( inputState.guessing==0 ) {
				
				info.setPackageSelections(new Selection((JavaToken)pkg),
				new Selection(id), id.getText(),
				new Selection((JavaToken)sem));
				
				definePackage(id);  // tell the symbol table about the package
				
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void importDefinition() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			match(LITERAL_import);
			identifierStar();
			match(SEMI);
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_1);
			} else {
			  throw ex;
			}
		}
	}
	
	public final void typeDefinition() throws RecognitionException, TokenStreamException {
		
		JavaBitSet mods;
		JavaToken commentToken = null;
		
		try {      // for error handling
			switch ( LA(1)) {
			case FINAL:
			case ABSTRACT:
			case STRICTFP:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_protected:
			case LITERAL_static:
			case LITERAL_transient:
			case LITERAL_native:
			case LITERAL_threadsafe:
			case LITERAL_synchronized:
			case LITERAL_volatile:
			case LITERAL_class:
			case LITERAL_interface:
			{
				if ( inputState.guessing==0 ) {
					commentToken = findAttachedComment((JavaToken)LT(1));
				}
				mods=modifiers();
				{
				switch ( LA(1)) {
				case LITERAL_class:
				{
					classDefinition(mods, commentToken);
					break;
				}
				case LITERAL_interface:
				{
					interfaceDefinition(mods, commentToken);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case SEMI:
			{
				match(SEMI);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (RecognitionException ex) {
			if (inputState.guessing==0) {
				reportError(ex);
				consume();
				consumeUntil(_tokenSet_2);
			} else {
			  throw ex;
			}
		}
	}
	
	public final JavaToken  identifier() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		Token  id1 = null;
		Token  id2 = null;
		t=null;
		
		id1 = LT(1);
		match(IDENT);
		if ( inputState.guessing==0 ) {
			t=(JavaToken)id1;
		}
		{
		_loop23:
		do {
			if ((LA(1)==DOT)) {
				match(DOT);
				id2 = LT(1);
				match(IDENT);
				if ( inputState.guessing==0 ) {
					t.setText(t.getText() + "." + id2.getText());
				}
			}
			else {
				break _loop23;
			}
			
		} while (true);
		}
		return t;
	}
	
	public final void identifierStar() throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		Token  id2 = null;
		String className=""; String packageName="";
		
		id = LT(1);
		match(IDENT);
		if ( inputState.guessing==0 ) {
			className=id.getText();
		}
		{
		_loop26:
		do {
			if ((LA(1)==DOT) && (LA(2)==IDENT)) {
				match(DOT);
				id2 = LT(1);
				match(IDENT);
				if ( inputState.guessing==0 ) {
					packageName += "."+className; className = id2.getText();
				}
			}
			else {
				break _loop26;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case DOT:
		{
			match(DOT);
			match(STAR);
			if ( inputState.guessing==0 ) {
				packageName += "."+className; className = null;
			}
			break;
		}
		case SEMI:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			
			// put the overall name in the token's text
			if (packageName.equals(""))
			id.setText(className);
			else if (className == null)
			id.setText(packageName.substring(1));
			else
			id.setText(packageName.substring(1) + "." + className);
			
			// tell the symbol table about the import
			addImport((JavaToken)id, className, packageName);
			
		}
	}
	
	public final JavaBitSet  modifiers() throws RecognitionException, TokenStreamException {
		JavaBitSet mods;
		
		mods = new JavaBitSet();
		
		{
		_loop30:
		do {
			if ((_tokenSet_3.member(LA(1)))) {
				modifier(mods);
			}
			else {
				break _loop30;
			}
			
		} while (true);
		}
		return mods;
	}
	
	public final void classDefinition(
		JavaBitSet mods, JavaToken commentToken
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		Token  ex = null;
		
		JavaToken superClass=null;
		JavaVector interfaces = new JavaVector();
		Vector interfaceSelections = new Vector();
		Selection extendsInsert=null, implementsInsert=null,
		extendsReplace=null, superReplace=null;
		
		
		match(LITERAL_class);
		id = LT(1);
		match(IDENT);
		if ( inputState.guessing==0 ) {
			
			// the place which we would want to insert an "extends" is at the
			// character just after the classname identifier
			// it is also potentially the place where we would insert a
			// "implements" so we will set that here and allow it to be overridden
			// later on if need be
			extendsInsert = implementsInsert = selectionAfterToken((JavaToken)id);
			
		}
		{
		switch ( LA(1)) {
		case LITERAL_extends:
		{
			ex = LT(1);
			match(LITERAL_extends);
			superClass=identifier();
			if ( inputState.guessing==0 ) {
				
				extendsReplace = new Selection((JavaToken)ex);
				superReplace = new Selection(superClass);
				
				// maybe we need to place "implements" lines after this superClass..
				// set it here
				implementsInsert = selectionAfterToken((JavaToken)superClass);
				
			}
			break;
		}
		case LCURLY:
		case LITERAL_implements:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case LITERAL_implements:
		{
			implementsInsert=implementsClause(interfaces, interfaceSelections);
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			defineClass( (JavaToken)id, superClass,
					  interfaces,
					  mods.get(MOD_ABSTRACT), mods.get(MOD_PUBLIC),
					  commentToken,
					  extendsInsert, implementsInsert,
					  extendsReplace, superReplace,
					  interfaceSelections);
		}
		classBlock();
		if ( inputState.guessing==0 ) {
			popScope();
		}
	}
	
	public final void interfaceDefinition(
		JavaBitSet mods, JavaToken commentToken
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		JavaVector superInterfaces = new JavaVector();
		Vector superInterfaceSelections = new Vector();
		Selection extendsInsert = null;
		
		
		match(LITERAL_interface);
		id = LT(1);
		match(IDENT);
		if ( inputState.guessing==0 ) {
			
				    // the place which we would want to insert an "extends" is at the
				    // character just after the interfacename identifier
				    extendsInsert = selectionAfterToken((JavaToken)id);
			
		}
		{
		switch ( LA(1)) {
		case LITERAL_extends:
		{
			extendsInsert=interfaceExtends(superInterfaces, superInterfaceSelections);
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			defineInterface((JavaToken)id,
					            superInterfaces,
					            mods.get(MOD_PUBLIC), commentToken,
					            extendsInsert, superInterfaceSelections);
		}
		classBlock();
		if ( inputState.guessing==0 ) {
			popScope();
		}
	}
	
	public final void declaration() throws RecognitionException, TokenStreamException {
		
		JavaToken type;
		
		modifiers();
		type=typeSpec();
		variableDefinitions(type, null);
	}
	
	public final JavaToken  typeSpec() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		t=null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			t=classTypeSpec();
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		{
			t=builtInTypeSpec();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final void variableDefinitions(
		JavaToken type, JavaToken commentToken
	) throws RecognitionException, TokenStreamException {
		
		
		variableDeclarator(type, commentToken);
		{
		_loop63:
		do {
			if ((LA(1)==COMMA)) {
				match(COMMA);
				variableDeclarator(type, commentToken);
			}
			else {
				break _loop63;
			}
			
		} while (true);
		}
	}
	
	public final JavaToken  classTypeSpec() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		t=null;
		
		t=identifier();
		{
		_loop15:
		do {
			if ((LA(1)==LBRACK)) {
				match(LBRACK);
				match(RBRACK);
				if ( inputState.guessing==0 ) {
					
					if(t != null)
					t.setText(t.getText() + "[]");
						
				}
			}
			else {
				break _loop15;
			}
			
		} while (true);
		}
		return t;
	}
	
	public final JavaToken  builtInTypeSpec() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		t=null;
		
		t=builtInType();
		{
		_loop18:
		do {
			if ((LA(1)==LBRACK)) {
				match(LBRACK);
				match(RBRACK);
				if ( inputState.guessing==0 ) {
					
							   if(t != null)
					t.setText(t.getText() + "[]");
							
				}
			}
			else {
				break _loop18;
			}
			
		} while (true);
		}
		return t;
	}
	
	public final JavaToken  builtInType() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		Token  bVoid = null;
		Token  bBoolean = null;
		Token  bByte = null;
		Token  bChar = null;
		Token  bShort = null;
		Token  bInt = null;
		Token  bFloat = null;
		Token  bLong = null;
		Token  bDouble = null;
		t=null;
		
		switch ( LA(1)) {
		case LITERAL_void:
		{
			bVoid = LT(1);
			match(LITERAL_void);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bVoid;
			}
			break;
		}
		case LITERAL_boolean:
		{
			bBoolean = LT(1);
			match(LITERAL_boolean);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bBoolean;
			}
			break;
		}
		case LITERAL_byte:
		{
			bByte = LT(1);
			match(LITERAL_byte);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bByte;
			}
			break;
		}
		case LITERAL_char:
		{
			bChar = LT(1);
			match(LITERAL_char);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bChar;
			}
			break;
		}
		case LITERAL_short:
		{
			bShort = LT(1);
			match(LITERAL_short);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bShort;
			}
			break;
		}
		case LITERAL_int:
		{
			bInt = LT(1);
			match(LITERAL_int);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bInt;
			}
			break;
		}
		case LITERAL_float:
		{
			bFloat = LT(1);
			match(LITERAL_float);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bFloat;
			}
			break;
		}
		case LITERAL_long:
		{
			bLong = LT(1);
			match(LITERAL_long);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bLong;
			}
			break;
		}
		case LITERAL_double:
		{
			bDouble = LT(1);
			match(LITERAL_double);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)bDouble;
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final JavaToken  type() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		t=null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			t=identifier();
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		{
			t=builtInType();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final void modifier(
		JavaBitSet mods
	) throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case LITERAL_private:
		{
			match(LITERAL_private);
			if ( inputState.guessing==0 ) {
				mods.set(MOD_PRIVATE);
			}
			break;
		}
		case LITERAL_public:
		{
			match(LITERAL_public);
			if ( inputState.guessing==0 ) {
				mods.set(MOD_PUBLIC);
			}
			break;
		}
		case LITERAL_protected:
		{
			match(LITERAL_protected);
			if ( inputState.guessing==0 ) {
				mods.set(MOD_PROTECTED);
			}
			break;
		}
		case LITERAL_static:
		{
			match(LITERAL_static);
			if ( inputState.guessing==0 ) {
				mods.set(MOD_STATIC);
			}
			break;
		}
		case LITERAL_transient:
		{
			match(LITERAL_transient);
			break;
		}
		case FINAL:
		{
			match(FINAL);
			break;
		}
		case ABSTRACT:
		{
			match(ABSTRACT);
			if ( inputState.guessing==0 ) {
				mods.set(MOD_ABSTRACT);
			}
			break;
		}
		case LITERAL_native:
		{
			match(LITERAL_native);
			break;
		}
		case LITERAL_threadsafe:
		{
			match(LITERAL_threadsafe);
			break;
		}
		case LITERAL_synchronized:
		{
			match(LITERAL_synchronized);
			break;
		}
		case LITERAL_volatile:
		{
			match(LITERAL_volatile);
			break;
		}
		case STRICTFP:
		{
			match(STRICTFP);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final Selection  implementsClause(
		JavaVector interfaces, Vector interfaceSelections
	) throws RecognitionException, TokenStreamException {
		Selection implementsInsert;
		
		Token  im = null;
		Token  co = null;
		JavaToken id;
		implementsInsert = null;
		
		
		im = LT(1);
		match(LITERAL_implements);
		id=identifier();
		if ( inputState.guessing==0 ) {
			
			implementsInsert = selectionAfterToken((JavaToken)id);
			
				  interfaceSelections.addElement(new Selection((JavaToken)im));
			interfaces.addElement(dummyClass(id));
				  interfaceSelections.addElement(new Selection((JavaToken)id));
			
		}
		{
		_loop45:
		do {
			if ((LA(1)==COMMA)) {
				co = LT(1);
				match(COMMA);
				id=identifier();
				if ( inputState.guessing==0 ) {
					
					implementsInsert = selectionAfterToken((JavaToken)id);
					
					interfaceSelections.addElement(new Selection((JavaToken)co));
					interfaces.addElement(dummyClass(id));
					interfaceSelections.addElement(new Selection((JavaToken)id));
					
				}
			}
			else {
				break _loop45;
			}
			
		} while (true);
		}
		return implementsInsert;
	}
	
	public final void classBlock() throws RecognitionException, TokenStreamException {
		
		
		match(LCURLY);
		{
		_loop39:
		do {
			switch ( LA(1)) {
			case FINAL:
			case ABSTRACT:
			case STRICTFP:
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_protected:
			case LITERAL_static:
			case LITERAL_transient:
			case LITERAL_native:
			case LITERAL_threadsafe:
			case LITERAL_synchronized:
			case LITERAL_volatile:
			case LITERAL_class:
			case LITERAL_interface:
			case LCURLY:
			{
				field();
				break;
			}
			case SEMI:
			{
				match(SEMI);
				break;
			}
			default:
			{
				break _loop39;
			}
			}
		} while (true);
		}
		match(RCURLY);
	}
	
	public final Selection  interfaceExtends(
		JavaVector interfaces, Vector interfaceSelections
	) throws RecognitionException, TokenStreamException {
		Selection extendsInsert;
		
		Token  ex = null;
		Token  co = null;
		JavaToken id;
		extendsInsert = null;
		
		
		ex = LT(1);
		match(LITERAL_extends);
		id=identifier();
		if ( inputState.guessing==0 ) {
			
			extendsInsert = selectionAfterToken((JavaToken)id);
			
			interfaceSelections.addElement(new Selection((JavaToken)ex));
				  interfaces.addElement(dummyClass(id));
				  interfaceSelections.addElement(new Selection((JavaToken)id));
			
		}
		{
		_loop42:
		do {
			if ((LA(1)==COMMA)) {
				co = LT(1);
				match(COMMA);
				id=identifier();
				if ( inputState.guessing==0 ) {
					
					extendsInsert = selectionAfterToken((JavaToken)id);
					
					interfaceSelections.addElement(new Selection((JavaToken)co));
					interfaces.addElement(dummyClass(id));
					interfaceSelections.addElement(new Selection((JavaToken)id));
					
				}
			}
			else {
				break _loop42;
			}
			
		} while (true);
		}
		return extendsInsert;
	}
	
	public final void field() throws RecognitionException, TokenStreamException {
		
		Token  method = null;
		
		JavaToken  type, commentToken = null;
		JavaVector exceptions = null;           // track thrown exceptions
		JavaBitSet mods = null;
		
		
		if ((_tokenSet_4.member(LA(1))) && (_tokenSet_5.member(LA(2)))) {
			if ( inputState.guessing==0 ) {
				commentToken = findAttachedComment((JavaToken)LT(1));
			}
			mods=modifiers();
			{
			switch ( LA(1)) {
			case LITERAL_class:
			{
				classDefinition(new JavaBitSet(), null);
				break;
			}
			case LITERAL_interface:
			{
				interfaceDefinition(new JavaBitSet(), null);
				break;
			}
			default:
				if ((LA(1)==IDENT) && (LA(2)==LPAREN)) {
					ctorHead(null, commentToken);
					constructorBody();
				}
				else if (((LA(1) >= LITERAL_void && LA(1) <= IDENT)) && (_tokenSet_6.member(LA(2)))) {
					type=typeSpec();
					{
					if ((LA(1)==IDENT) && (LA(2)==LPAREN)) {
						method = LT(1);
						match(IDENT);
						if ( inputState.guessing==0 ) {
							
									        // tell the symbol table about it.  Note that this signals that
								        // we are in a method header so we handle parameters appropriately
								        defineMethod((JavaToken)method, type, commentToken);
							
						}
						match(LPAREN);
						parameterDeclarationList();
						match(RPAREN);
						{
						_loop50:
						do {
							if ((LA(1)==LBRACK)) {
								match(LBRACK);
								match(RBRACK);
							}
							else {
								break _loop50;
							}
							
						} while (true);
						}
						{
						switch ( LA(1)) {
						case LITERAL_throws:
						{
							exceptions=throwsClause();
							break;
						}
						case SEMI:
						case LCURLY:
						{
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
						if ( inputState.guessing==0 ) {
							endMethodHead(exceptions);
						}
						{
						switch ( LA(1)) {
						case LCURLY:
						{
							compoundStatement(BODY);
							break;
						}
						case SEMI:
						{
							match(SEMI);
							if ( inputState.guessing==0 ) {
								popScope();
							}
							break;
						}
						default:
						{
							throw new NoViableAltException(LT(1), getFilename());
						}
						}
						}
					}
					else if ((LA(1)==IDENT) && (_tokenSet_7.member(LA(2)))) {
						variableDefinitions(type,commentToken);
						match(SEMI);
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
				}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
		}
		else if ((LA(1)==LITERAL_static) && (LA(2)==LCURLY)) {
			match(LITERAL_static);
			compoundStatement(CLASS_INIT);
		}
		else if ((LA(1)==LCURLY)) {
			compoundStatement(INSTANCE_INIT);
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
	}
	
	public final void ctorHead(
		JavaToken type, JavaToken commentToken
	) throws RecognitionException, TokenStreamException {
		
		Token  method = null;
		
		JavaVector exceptions=null;
		
		
		method = LT(1);
		match(IDENT);
		if ( inputState.guessing==0 ) {
			
					// tell the symbol table about it.  Note that this signals that
				// we are in a method header so we handle parameters appropriately
				defineMethod((JavaToken)method, type, commentToken);
			
		}
		match(LPAREN);
		parameterDeclarationList();
		match(RPAREN);
		{
		switch ( LA(1)) {
		case LITERAL_throws:
		{
			exceptions=throwsClause();
			break;
		}
		case LCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		if ( inputState.guessing==0 ) {
			
			endMethodHead(exceptions);
			
		}
	}
	
	public final void constructorBody() throws RecognitionException, TokenStreamException {
		
		
		match(LCURLY);
		{
		boolean synPredMatched56 = false;
		if (((_tokenSet_8.member(LA(1))) && (_tokenSet_9.member(LA(2))))) {
			int _m56 = mark();
			synPredMatched56 = true;
			inputState.guessing++;
			try {
				{
				explicitConstructorInvocation();
				}
			}
			catch (RecognitionException pe) {
				synPredMatched56 = false;
			}
			rewind(_m56);
			inputState.guessing--;
		}
		if ( synPredMatched56 ) {
			explicitConstructorInvocation();
		}
		else if ((_tokenSet_10.member(LA(1))) && (_tokenSet_11.member(LA(2)))) {
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		{
		_loop58:
		do {
			if ((_tokenSet_12.member(LA(1)))) {
				statement();
			}
			else {
				break _loop58;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			popScope();
		}
		match(RCURLY);
	}
	
	public final void parameterDeclarationList() throws RecognitionException, TokenStreamException {
		
		
		{
		switch ( LA(1)) {
		case FINAL:
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		{
			parameterDeclaration();
			{
			_loop83:
			do {
				if ((LA(1)==COMMA)) {
					match(COMMA);
					parameterDeclaration();
				}
				else {
					break _loop83;
				}
				
			} while (true);
			}
			break;
		}
		case RPAREN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final JavaVector  throwsClause() throws RecognitionException, TokenStreamException {
		JavaVector exceptions;
		
		JavaToken id; exceptions = new JavaVector();
		
		match(LITERAL_throws);
		id=identifier();
		if ( inputState.guessing==0 ) {
			exceptions.addElement(dummyClass(id));
		}
		{
		_loop79:
		do {
			if ((LA(1)==COMMA)) {
				match(COMMA);
				id=identifier();
				if ( inputState.guessing==0 ) {
					exceptions.addElement(dummyClass(id));
				}
			}
			else {
				break _loop79;
			}
			
		} while (true);
		}
		return exceptions;
	}
	
	public final void compoundStatement(
		int scopeType
	) throws RecognitionException, TokenStreamException {
		
		Token  lc = null;
		
		lc = LT(1);
		match(LCURLY);
		if ( inputState.guessing==0 ) {
			// based on the scopeType we are processing
			switch(scopeType) {
			// if it's a new block, tell the symbol table
			case NEW_SCOPE:
			defineBlock((JavaToken)lc);
			break;
			
			// if it's a class initializer or instance initializer,
			//   treat it like a method with a special name
			case CLASS_INIT:
			lc.setText("~class-init~");
			defineMethod(null, (JavaToken)lc, null);
			endMethodHead(null);
			break;
			case INSTANCE_INIT:
			lc.setText("~instance-init~");
			defineMethod(null, (JavaToken)lc, null);
			endMethodHead(null);
			break;
			
			// otherwise, it's a body, so do nothing special
			}
			
		}
		{
		_loop91:
		do {
			if ((_tokenSet_12.member(LA(1)))) {
				statement();
			}
			else {
				break _loop91;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			popScope();
		}
		match(RCURLY);
	}
	
	public final void explicitConstructorInvocation() throws RecognitionException, TokenStreamException {
		
		
		{
		if ((LA(1)==LITERAL_this) && (LA(2)==LPAREN)) {
			match(LITERAL_this);
			match(LPAREN);
			argList();
			match(RPAREN);
			match(SEMI);
		}
		else if ((LA(1)==LITERAL_super) && (LA(2)==LPAREN)) {
			match(LITERAL_super);
			match(LPAREN);
			argList();
			match(RPAREN);
			match(SEMI);
		}
		else if ((_tokenSet_8.member(LA(1))) && (_tokenSet_9.member(LA(2)))) {
			primaryExpression();
			match(DOT);
			match(LITERAL_super);
			match(LPAREN);
			argList();
			match(RPAREN);
			match(SEMI);
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
	}
	
	public final void statement() throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		Token  bid = null;
		Token  cid = null;
		int count = -1; // used for parameter counts in method calls
		JavaBitSet mods;
		
		switch ( LA(1)) {
		case LCURLY:
		{
			compoundStatement(NEW_SCOPE);
			break;
		}
		case LITERAL_if:
		{
			match(LITERAL_if);
			match(LPAREN);
			expression();
			match(RPAREN);
			statement();
			{
			if ((LA(1)==LITERAL_else) && (_tokenSet_12.member(LA(2)))) {
				match(LITERAL_else);
				statement();
			}
			else if ((_tokenSet_13.member(LA(1))) && (_tokenSet_14.member(LA(2)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			break;
		}
		case LITERAL_for:
		{
			match(LITERAL_for);
			match(LPAREN);
			{
			switch ( LA(1)) {
			case FINAL:
			case ABSTRACT:
			case STRICTFP:
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case LITERAL_private:
			case LITERAL_public:
			case LITERAL_protected:
			case LITERAL_static:
			case LITERAL_transient:
			case LITERAL_native:
			case LITERAL_threadsafe:
			case LITERAL_synchronized:
			case LITERAL_volatile:
			case LPAREN:
			case LITERAL_this:
			case LITERAL_super:
			case PLUS:
			case MINUS:
			case INC:
			case DEC:
			case BNOT:
			case LNOT:
			case LITERAL_true:
			case LITERAL_false:
			case LITERAL_null:
			case LITERAL_new:
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				forInit();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			{
			switch ( LA(1)) {
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case LPAREN:
			case LITERAL_this:
			case LITERAL_super:
			case PLUS:
			case MINUS:
			case INC:
			case DEC:
			case BNOT:
			case LNOT:
			case LITERAL_true:
			case LITERAL_false:
			case LITERAL_null:
			case LITERAL_new:
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				expression();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			{
			switch ( LA(1)) {
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case LPAREN:
			case LITERAL_this:
			case LITERAL_super:
			case PLUS:
			case MINUS:
			case INC:
			case DEC:
			case BNOT:
			case LNOT:
			case LITERAL_true:
			case LITERAL_false:
			case LITERAL_null:
			case LITERAL_new:
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				count=expressionList();
				break;
			}
			case RPAREN:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAREN);
			statement();
			break;
		}
		case LITERAL_while:
		{
			match(LITERAL_while);
			match(LPAREN);
			expression();
			match(RPAREN);
			statement();
			break;
		}
		case LITERAL_do:
		{
			match(LITERAL_do);
			statement();
			match(LITERAL_while);
			match(LPAREN);
			expression();
			match(RPAREN);
			match(SEMI);
			break;
		}
		case LITERAL_break:
		{
			match(LITERAL_break);
			{
			switch ( LA(1)) {
			case IDENT:
			{
				bid = LT(1);
				match(IDENT);
				if ( inputState.guessing==0 ) {
					reference((JavaToken)bid);
				}
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			break;
		}
		case LITERAL_continue:
		{
			match(LITERAL_continue);
			{
			switch ( LA(1)) {
			case IDENT:
			{
				cid = LT(1);
				match(IDENT);
				if ( inputState.guessing==0 ) {
					reference((JavaToken)cid);
				}
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			break;
		}
		case LITERAL_return:
		{
			match(LITERAL_return);
			{
			switch ( LA(1)) {
			case LITERAL_void:
			case LITERAL_boolean:
			case LITERAL_byte:
			case LITERAL_char:
			case LITERAL_short:
			case LITERAL_int:
			case LITERAL_float:
			case LITERAL_long:
			case LITERAL_double:
			case IDENT:
			case LPAREN:
			case LITERAL_this:
			case LITERAL_super:
			case PLUS:
			case MINUS:
			case INC:
			case DEC:
			case BNOT:
			case LNOT:
			case LITERAL_true:
			case LITERAL_false:
			case LITERAL_null:
			case LITERAL_new:
			case NUM_INT:
			case CHAR_LITERAL:
			case STRING_LITERAL:
			case NUM_FLOAT:
			case NUM_LONG:
			case NUM_DOUBLE:
			{
				expression();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			break;
		}
		case LITERAL_switch:
		{
			match(LITERAL_switch);
			match(LPAREN);
			expression();
			match(RPAREN);
			match(LCURLY);
			{
			_loop103:
			do {
				if ((LA(1)==LITERAL_case||LA(1)==LITERAL_default)) {
					casesGroup();
				}
				else {
					break _loop103;
				}
				
			} while (true);
			}
			match(RCURLY);
			break;
		}
		case LITERAL_try:
		{
			tryBlock();
			break;
		}
		case LITERAL_throw:
		{
			match(LITERAL_throw);
			expression();
			match(SEMI);
			break;
		}
		case LITERAL_assert:
		{
			match(LITERAL_assert);
			expression();
			{
			switch ( LA(1)) {
			case COLON:
			{
				match(COLON);
				expression();
				break;
			}
			case SEMI:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(SEMI);
			break;
		}
		case SEMI:
		{
			match(SEMI);
			break;
		}
		default:
			boolean synPredMatched94 = false;
			if (((_tokenSet_15.member(LA(1))) && (_tokenSet_16.member(LA(2))))) {
				int _m94 = mark();
				synPredMatched94 = true;
				inputState.guessing++;
				try {
					{
					declaration();
					}
				}
				catch (RecognitionException pe) {
					synPredMatched94 = false;
				}
				rewind(_m94);
				inputState.guessing--;
			}
			if ( synPredMatched94 ) {
				declaration();
				match(SEMI);
			}
			else if ((_tokenSet_17.member(LA(1))) && (_tokenSet_18.member(LA(2)))) {
				expression();
				match(SEMI);
			}
			else if ((_tokenSet_19.member(LA(1))) && (_tokenSet_20.member(LA(2)))) {
				mods=modifiers();
				classDefinition(mods, null);
			}
			else if ((LA(1)==IDENT) && (LA(2)==COLON)) {
				id = LT(1);
				match(IDENT);
				match(COLON);
				statement();
				if ( inputState.guessing==0 ) {
					defineLabel((JavaToken)id);
				}
			}
			else if ((LA(1)==LITERAL_synchronized) && (LA(2)==LPAREN)) {
				match(LITERAL_synchronized);
				match(LPAREN);
				expression();
				match(RPAREN);
				statement();
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final int  argList() throws RecognitionException, TokenStreamException {
		int count;
		
		count=0;
		
		{
		switch ( LA(1)) {
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			count=expressionList();
			break;
		}
		case RPAREN:
		{
			if ( inputState.guessing==0 ) {
				count=0;
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return count;
	}
	
	public final JavaToken  primaryExpression() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		Token  id = null;
		Token  th = null;
		Token  s = null;
		t=null;
		
		switch ( LA(1)) {
		case IDENT:
		{
			id = LT(1);
			match(IDENT);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)id;
			}
			break;
		}
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			constant();
			break;
		}
		case LITERAL_true:
		{
			match(LITERAL_true);
			break;
		}
		case LITERAL_false:
		{
			match(LITERAL_false);
			break;
		}
		case LITERAL_this:
		{
			th = LT(1);
			match(LITERAL_this);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)th; setNearestClassScope();
			}
			break;
		}
		case LITERAL_null:
		{
			match(LITERAL_null);
			break;
		}
		case LITERAL_new:
		{
			t=newExpression();
			break;
		}
		case LPAREN:
		{
			match(LPAREN);
			expression();
			match(RPAREN);
			break;
		}
		case LITERAL_super:
		{
			s = LT(1);
			match(LITERAL_super);
			if ( inputState.guessing==0 ) {
				t = (JavaToken)s;
			}
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		{
			builtInType();
			{
			_loop180:
			do {
				if ((LA(1)==LBRACK)) {
					match(LBRACK);
					match(RBRACK);
				}
				else {
					break _loop180;
				}
				
			} while (true);
			}
			match(DOT);
			match(LITERAL_class);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return t;
	}
	
	public final void variableDeclarator(
		JavaToken type, JavaToken commentToken
	) throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		
		id = LT(1);
		match(IDENT);
		{
		_loop66:
		do {
			if ((LA(1)==LBRACK)) {
				match(LBRACK);
				match(RBRACK);
				if ( inputState.guessing==0 ) {
					if(type != null)
										type.setText(type.getText() + "[]");
									
				}
			}
			else {
				break _loop66;
			}
			
		} while (true);
		}
		varInitializer();
		if ( inputState.guessing==0 ) {
			defineVar((JavaToken)id, type, commentToken);
		}
	}
	
	public final void varInitializer() throws RecognitionException, TokenStreamException {
		
		
		{
		switch ( LA(1)) {
		case ASSIGN:
		{
			match(ASSIGN);
			initializer();
			break;
		}
		case SEMI:
		case COMMA:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void initializer() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			expression();
			break;
		}
		case LCURLY:
		{
			arrayInitializer();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void arrayInitializer() throws RecognitionException, TokenStreamException {
		
		
		match(LCURLY);
		{
		switch ( LA(1)) {
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LCURLY:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			initializer();
			{
			_loop72:
			do {
				if ((LA(1)==COMMA) && (_tokenSet_21.member(LA(2)))) {
					match(COMMA);
					initializer();
				}
				else {
					break _loop72;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case COMMA:
			{
				match(COMMA);
				break;
			}
			case RCURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case RCURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(RCURLY);
	}
	
	public final void expression() throws RecognitionException, TokenStreamException {
		
		
		assignmentExpression();
	}
	
	public final void parameterDeclaration() throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		JavaToken type;
		
		parameterModifier();
		type=typeSpec();
		id = LT(1);
		match(IDENT);
		{
		_loop86:
		do {
			if ((LA(1)==LBRACK)) {
				match(LBRACK);
				match(RBRACK);
				if ( inputState.guessing==0 ) {
					if(type != null)
								       type.setText(type.getText() + "[]");
				}
			}
			else {
				break _loop86;
			}
			
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			defineVar((JavaToken)id, type, null);
		}
	}
	
	public final void parameterModifier() throws RecognitionException, TokenStreamException {
		
		Token  f = null;
		
		{
		switch ( LA(1)) {
		case FINAL:
		{
			f = LT(1);
			match(FINAL);
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void forInit() throws RecognitionException, TokenStreamException {
		
		int count = -1;
		
		boolean synPredMatched115 = false;
		if (((_tokenSet_15.member(LA(1))) && (_tokenSet_16.member(LA(2))))) {
			int _m115 = mark();
			synPredMatched115 = true;
			inputState.guessing++;
			try {
				{
				declaration();
				}
			}
			catch (RecognitionException pe) {
				synPredMatched115 = false;
			}
			rewind(_m115);
			inputState.guessing--;
		}
		if ( synPredMatched115 ) {
			declaration();
		}
		else if ((_tokenSet_17.member(LA(1))) && (_tokenSet_22.member(LA(2)))) {
			count=expressionList();
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
	}
	
	public final int  expressionList() throws RecognitionException, TokenStreamException {
		int count;
		
		count=1;
		
		expression();
		{
		_loop124:
		do {
			if ((LA(1)==COMMA)) {
				match(COMMA);
				expression();
				if ( inputState.guessing==0 ) {
					count++;
				}
			}
			else {
				break _loop124;
			}
			
		} while (true);
		}
		return count;
	}
	
	public final void casesGroup() throws RecognitionException, TokenStreamException {
		
		
		{
		int _cnt107=0;
		_loop107:
		do {
			if ((LA(1)==LITERAL_case||LA(1)==LITERAL_default) && (_tokenSet_23.member(LA(2)))) {
				aCase();
			}
			else {
				if ( _cnt107>=1 ) { break _loop107; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt107++;
		} while (true);
		}
		caseSList();
	}
	
	public final void tryBlock() throws RecognitionException, TokenStreamException {
		
		
		match(LITERAL_try);
		compoundStatement(NEW_SCOPE);
		{
		_loop118:
		do {
			if ((LA(1)==LITERAL_catch)) {
				handler();
			}
			else {
				break _loop118;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case LITERAL_finally:
		{
			match(LITERAL_finally);
			compoundStatement(NEW_SCOPE);
			break;
		}
		case FINAL:
		case ABSTRACT:
		case STRICTFP:
		case SEMI:
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LITERAL_private:
		case LITERAL_public:
		case LITERAL_protected:
		case LITERAL_static:
		case LITERAL_transient:
		case LITERAL_native:
		case LITERAL_threadsafe:
		case LITERAL_synchronized:
		case LITERAL_volatile:
		case LITERAL_class:
		case LCURLY:
		case RCURLY:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case LITERAL_if:
		case LITERAL_else:
		case LITERAL_for:
		case LITERAL_while:
		case LITERAL_do:
		case LITERAL_break:
		case LITERAL_continue:
		case LITERAL_return:
		case LITERAL_switch:
		case LITERAL_throw:
		case LITERAL_assert:
		case LITERAL_case:
		case LITERAL_default:
		case LITERAL_try:
		case PLUS:
		case MINUS:
		case INC:
		case DEC:
		case BNOT:
		case LNOT:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void aCase() throws RecognitionException, TokenStreamException {
		
		
		{
		switch ( LA(1)) {
		case LITERAL_case:
		{
			match(LITERAL_case);
			expression();
			break;
		}
		case LITERAL_default:
		{
			match(LITERAL_default);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(COLON);
	}
	
	public final void caseSList() throws RecognitionException, TokenStreamException {
		
		
		{
		_loop112:
		do {
			if ((_tokenSet_12.member(LA(1)))) {
				statement();
			}
			else {
				break _loop112;
			}
			
		} while (true);
		}
	}
	
	public final void handler() throws RecognitionException, TokenStreamException {
		
		
		match(LITERAL_catch);
		match(LPAREN);
		parameterDeclaration();
		match(RPAREN);
		compoundStatement(NEW_SCOPE);
	}
	
	public final void assignmentExpression() throws RecognitionException, TokenStreamException {
		
		
		conditionalExpression();
		{
		switch ( LA(1)) {
		case ASSIGN:
		case PLUS_ASSIGN:
		case MINUS_ASSIGN:
		case STAR_ASSIGN:
		case DIV_ASSIGN:
		case MOD_ASSIGN:
		case SR_ASSIGN:
		case BSR_ASSIGN:
		case SL_ASSIGN:
		case BAND_ASSIGN:
		case BXOR_ASSIGN:
		case BOR_ASSIGN:
		{
			{
			switch ( LA(1)) {
			case ASSIGN:
			{
				match(ASSIGN);
				break;
			}
			case PLUS_ASSIGN:
			{
				match(PLUS_ASSIGN);
				break;
			}
			case MINUS_ASSIGN:
			{
				match(MINUS_ASSIGN);
				break;
			}
			case STAR_ASSIGN:
			{
				match(STAR_ASSIGN);
				break;
			}
			case DIV_ASSIGN:
			{
				match(DIV_ASSIGN);
				break;
			}
			case MOD_ASSIGN:
			{
				match(MOD_ASSIGN);
				break;
			}
			case SR_ASSIGN:
			{
				match(SR_ASSIGN);
				break;
			}
			case BSR_ASSIGN:
			{
				match(BSR_ASSIGN);
				break;
			}
			case SL_ASSIGN:
			{
				match(SL_ASSIGN);
				break;
			}
			case BAND_ASSIGN:
			{
				match(BAND_ASSIGN);
				break;
			}
			case BXOR_ASSIGN:
			{
				match(BXOR_ASSIGN);
				break;
			}
			case BOR_ASSIGN:
			{
				match(BOR_ASSIGN);
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			assignmentExpression();
			break;
		}
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case COLON:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void conditionalExpression() throws RecognitionException, TokenStreamException {
		
		
		logicalOrExpression();
		{
		switch ( LA(1)) {
		case QUESTION:
		{
			match(QUESTION);
			assignmentExpression();
			match(COLON);
			conditionalExpression();
			break;
		}
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case ASSIGN:
		case COLON:
		case PLUS_ASSIGN:
		case MINUS_ASSIGN:
		case STAR_ASSIGN:
		case DIV_ASSIGN:
		case MOD_ASSIGN:
		case SR_ASSIGN:
		case BSR_ASSIGN:
		case SL_ASSIGN:
		case BAND_ASSIGN:
		case BXOR_ASSIGN:
		case BOR_ASSIGN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void logicalOrExpression() throws RecognitionException, TokenStreamException {
		
		
		logicalAndExpression();
		{
		_loop132:
		do {
			if ((LA(1)==LOR)) {
				match(LOR);
				logicalAndExpression();
			}
			else {
				break _loop132;
			}
			
		} while (true);
		}
	}
	
	public final void logicalAndExpression() throws RecognitionException, TokenStreamException {
		
		
		inclusiveOrExpression();
		{
		_loop135:
		do {
			if ((LA(1)==LAND)) {
				match(LAND);
				inclusiveOrExpression();
			}
			else {
				break _loop135;
			}
			
		} while (true);
		}
	}
	
	public final void inclusiveOrExpression() throws RecognitionException, TokenStreamException {
		
		
		exclusiveOrExpression();
		{
		_loop138:
		do {
			if ((LA(1)==BOR)) {
				match(BOR);
				exclusiveOrExpression();
			}
			else {
				break _loop138;
			}
			
		} while (true);
		}
	}
	
	public final void exclusiveOrExpression() throws RecognitionException, TokenStreamException {
		
		
		andExpression();
		{
		_loop141:
		do {
			if ((LA(1)==BXOR)) {
				match(BXOR);
				andExpression();
			}
			else {
				break _loop141;
			}
			
		} while (true);
		}
	}
	
	public final void andExpression() throws RecognitionException, TokenStreamException {
		
		
		equalityExpression();
		{
		_loop144:
		do {
			if ((LA(1)==BAND)) {
				match(BAND);
				equalityExpression();
			}
			else {
				break _loop144;
			}
			
		} while (true);
		}
	}
	
	public final void equalityExpression() throws RecognitionException, TokenStreamException {
		
		
		relationalExpression();
		{
		_loop148:
		do {
			if ((LA(1)==NOT_EQUAL||LA(1)==EQUAL)) {
				{
				switch ( LA(1)) {
				case NOT_EQUAL:
				{
					match(NOT_EQUAL);
					break;
				}
				case EQUAL:
				{
					match(EQUAL);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				relationalExpression();
			}
			else {
				break _loop148;
			}
			
		} while (true);
		}
	}
	
	public final void relationalExpression() throws RecognitionException, TokenStreamException {
		
		
		shiftExpression();
		{
		switch ( LA(1)) {
		case SEMI:
		case RBRACK:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case ASSIGN:
		case COLON:
		case PLUS_ASSIGN:
		case MINUS_ASSIGN:
		case STAR_ASSIGN:
		case DIV_ASSIGN:
		case MOD_ASSIGN:
		case SR_ASSIGN:
		case BSR_ASSIGN:
		case SL_ASSIGN:
		case BAND_ASSIGN:
		case BXOR_ASSIGN:
		case BOR_ASSIGN:
		case QUESTION:
		case LOR:
		case LAND:
		case BOR:
		case BXOR:
		case BAND:
		case NOT_EQUAL:
		case EQUAL:
		case LT:
		case GT:
		case LE:
		case GE:
		{
			{
			_loop153:
			do {
				if (((LA(1) >= LT && LA(1) <= GE))) {
					{
					switch ( LA(1)) {
					case LT:
					{
						match(LT);
						break;
					}
					case GT:
					{
						match(GT);
						break;
					}
					case LE:
					{
						match(LE);
						break;
					}
					case GE:
					{
						match(GE);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					shiftExpression();
				}
				else {
					break _loop153;
				}
				
			} while (true);
			}
			break;
		}
		case LITERAL_instanceof:
		{
			match(LITERAL_instanceof);
			typeSpec();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void shiftExpression() throws RecognitionException, TokenStreamException {
		
		
		additiveExpression();
		{
		_loop157:
		do {
			if (((LA(1) >= SL && LA(1) <= BSR))) {
				{
				switch ( LA(1)) {
				case SL:
				{
					match(SL);
					break;
				}
				case SR:
				{
					match(SR);
					break;
				}
				case BSR:
				{
					match(BSR);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				additiveExpression();
			}
			else {
				break _loop157;
			}
			
		} while (true);
		}
	}
	
	public final void additiveExpression() throws RecognitionException, TokenStreamException {
		
		
		multiplicativeExpression();
		{
		_loop161:
		do {
			if ((LA(1)==PLUS||LA(1)==MINUS)) {
				{
				switch ( LA(1)) {
				case PLUS:
				{
					match(PLUS);
					break;
				}
				case MINUS:
				{
					match(MINUS);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				multiplicativeExpression();
			}
			else {
				break _loop161;
			}
			
		} while (true);
		}
	}
	
	public final void multiplicativeExpression() throws RecognitionException, TokenStreamException {
		
		
		unaryExpression();
		{
		_loop165:
		do {
			if ((_tokenSet_24.member(LA(1)))) {
				{
				switch ( LA(1)) {
				case STAR:
				{
					match(STAR);
					break;
				}
				case DIV:
				{
					match(DIV);
					break;
				}
				case MOD:
				{
					match(MOD);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				unaryExpression();
			}
			else {
				break _loop165;
			}
			
		} while (true);
		}
	}
	
	public final void unaryExpression() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case INC:
		{
			match(INC);
			unaryExpression();
			break;
		}
		case DEC:
		{
			match(DEC);
			unaryExpression();
			break;
		}
		case MINUS:
		{
			match(MINUS);
			unaryExpression();
			break;
		}
		case PLUS:
		{
			match(PLUS);
			unaryExpression();
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case BNOT:
		case LNOT:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			unaryExpressionNotPlusMinus();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void unaryExpressionNotPlusMinus() throws RecognitionException, TokenStreamException {
		
		JavaToken t;
		
		switch ( LA(1)) {
		case BNOT:
		{
			match(BNOT);
			unaryExpression();
			break;
		}
		case LNOT:
		{
			match(LNOT);
			unaryExpression();
			break;
		}
		case LITERAL_void:
		case LITERAL_boolean:
		case LITERAL_byte:
		case LITERAL_char:
		case LITERAL_short:
		case LITERAL_int:
		case LITERAL_float:
		case LITERAL_long:
		case LITERAL_double:
		case IDENT:
		case LPAREN:
		case LITERAL_this:
		case LITERAL_super:
		case LITERAL_true:
		case LITERAL_false:
		case LITERAL_null:
		case LITERAL_new:
		case NUM_INT:
		case CHAR_LITERAL:
		case STRING_LITERAL:
		case NUM_FLOAT:
		case NUM_LONG:
		case NUM_DOUBLE:
		{
			{
			if ((LA(1)==LPAREN) && ((LA(2) >= LITERAL_void && LA(2) <= LITERAL_double))) {
				match(LPAREN);
				t=builtInTypeSpec();
				match(RPAREN);
				unaryExpression();
				if ( inputState.guessing==0 ) {
					
					reference(t);
					
				}
			}
			else {
				boolean synPredMatched170 = false;
				if (((LA(1)==LPAREN) && (LA(2)==IDENT))) {
					int _m170 = mark();
					synPredMatched170 = true;
					inputState.guessing++;
					try {
						{
						match(LPAREN);
						t=classTypeSpec();
						match(RPAREN);
						unaryExpressionNotPlusMinus();
						}
					}
					catch (RecognitionException pe) {
						synPredMatched170 = false;
					}
					rewind(_m170);
					inputState.guessing--;
				}
				if ( synPredMatched170 ) {
					match(LPAREN);
					t=classTypeSpec();
					match(RPAREN);
					unaryExpressionNotPlusMinus();
					if ( inputState.guessing==0 ) {
						
						reference(t);
						
					}
				}
				else if ((_tokenSet_8.member(LA(1))) && (_tokenSet_25.member(LA(2)))) {
					postfixExpression();
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		
	public final void postfixExpression() throws RecognitionException, TokenStreamException {
		
		Token  id = null;
		JavaToken t; int count=-1;
		
		t=primaryExpression();
		{
		_loop176:
		do {
			switch ( LA(1)) {
			case DOT:
			{
				match(DOT);
				{
				switch ( LA(1)) {
				case IDENT:
				{
					id = LT(1);
					match(IDENT);
					if ( inputState.guessing==0 ) {
						if (t!=null) t.setText(t.getText()+"."+id.getText());
					}
					break;
				}
				case LITERAL_this:
				{
					match(LITERAL_this);
					if ( inputState.guessing==0 ) {
						if (t!=null) t.setText(t.getText()+".this");
					}
					break;
				}
				case LITERAL_class:
				{
					match(LITERAL_class);
					if ( inputState.guessing==0 ) {
						if (t!=null) t.setText(t.getText()+".class");
					}
					break;
				}
				case LITERAL_new:
				{
					newExpression();
					break;
				}
				case LITERAL_super:
				{
					match(LITERAL_super);
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case LPAREN:
			{
				match(LPAREN);
				count=argList();
				match(RPAREN);
				if ( inputState.guessing==0 ) {
					
						                if (t!=null)
						                    t.setParamCount(count);
						
				}
				break;
			}
			default:
				if ((LA(1)==LBRACK) && (LA(2)==RBRACK)) {
					{
					int _cnt175=0;
					_loop175:
					do {
						if ((LA(1)==LBRACK)) {
							match(LBRACK);
							match(RBRACK);
						}
						else {
							if ( _cnt175>=1 ) { break _loop175; } else {throw new NoViableAltException(LT(1), getFilename());}
						}
						
						_cnt175++;
					} while (true);
					}
					match(DOT);
					match(LITERAL_class);
				}
				else if ((LA(1)==LBRACK) && (_tokenSet_17.member(LA(2)))) {
					match(LBRACK);
					expression();
					match(RBRACK);
				}
			else {
				break _loop176;
			}
			}
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			if (t != null) reference(t);
		}
		{
		switch ( LA(1)) {
		case INC:
		{
			match(INC);
			break;
		}
		case DEC:
		{
			match(DEC);
			break;
		}
		case SEMI:
		case RBRACK:
		case STAR:
		case RCURLY:
		case COMMA:
		case RPAREN:
		case ASSIGN:
		case COLON:
		case PLUS_ASSIGN:
		case MINUS_ASSIGN:
		case STAR_ASSIGN:
		case DIV_ASSIGN:
		case MOD_ASSIGN:
		case SR_ASSIGN:
		case BSR_ASSIGN:
		case SL_ASSIGN:
		case BAND_ASSIGN:
		case BXOR_ASSIGN:
		case BOR_ASSIGN:
		case QUESTION:
		case LOR:
		case LAND:
		case BOR:
		case BXOR:
		case BAND:
		case NOT_EQUAL:
		case EQUAL:
		case LT:
		case GT:
		case LE:
		case GE:
		case LITERAL_instanceof:
		case SL:
		case SR:
		case BSR:
		case PLUS:
		case MINUS:
		case DIV:
		case MOD:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
/** object instantiation.
 *  Trees are built as illustrated by the following input/tree pairs:
 *
 *  new T()
 *
 *  new
 *   |
 *   T --  ELIST
 *           |
 *          arg1 -- arg2 -- .. -- argn
 *
 *  new int[]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *
 *  new int[] {1,2}
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR -- ARRAY_INIT
 *                                  |
 *                                EXPR -- EXPR
 *                                  |      |
 *                                  1      2
 *
 *  new int[3]
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *                |
 *              EXPR
 *                |
 *                3
 *
 *  new int[1][2]
 *
 *  new
 *   |
 *  int -- ARRAY_DECLARATOR
 *               |
 *         ARRAY_DECLARATOR -- EXPR
 *               |              |
 *             EXPR             1
 *               |
 *               2
 *
 */
	public final JavaToken  newExpression() throws RecognitionException, TokenStreamException {
		JavaToken t;
		
		t=null; int count=-1;
		
		match(LITERAL_new);
		t=type();
		{
		switch ( LA(1)) {
		case LPAREN:
		{
			match(LPAREN);
			count=argList();
			match(RPAREN);
			if ( inputState.guessing==0 ) {
				
				t.setText(t.getText()+".~constructor~");
				t.setParamCount(count);
				
			}
			{
			switch ( LA(1)) {
			case LCURLY:
			{
				classBlock();
				break;
			}
			case SEMI:
			case LBRACK:
			case RBRACK:
			case DOT:
			case STAR:
			case RCURLY:
			case COMMA:
			case LPAREN:
			case RPAREN:
			case ASSIGN:
			case COLON:
			case PLUS_ASSIGN:
			case MINUS_ASSIGN:
			case STAR_ASSIGN:
			case DIV_ASSIGN:
			case MOD_ASSIGN:
			case SR_ASSIGN:
			case BSR_ASSIGN:
			case SL_ASSIGN:
			case BAND_ASSIGN:
			case BXOR_ASSIGN:
			case BOR_ASSIGN:
			case QUESTION:
			case LOR:
			case LAND:
			case BOR:
			case BXOR:
			case BAND:
			case NOT_EQUAL:
			case EQUAL:
			case LT:
			case GT:
			case LE:
			case GE:
			case LITERAL_instanceof:
			case SL:
			case SR:
			case BSR:
			case PLUS:
			case MINUS:
			case DIV:
			case MOD:
			case INC:
			case DEC:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		case LBRACK:
		{
			newArrayDeclarator();
			{
			switch ( LA(1)) {
			case LCURLY:
			{
				arrayInitializer();
				break;
			}
			case SEMI:
			case LBRACK:
			case RBRACK:
			case DOT:
			case STAR:
			case RCURLY:
			case COMMA:
			case LPAREN:
			case RPAREN:
			case ASSIGN:
			case COLON:
			case PLUS_ASSIGN:
			case MINUS_ASSIGN:
			case STAR_ASSIGN:
			case DIV_ASSIGN:
			case MOD_ASSIGN:
			case SR_ASSIGN:
			case BSR_ASSIGN:
			case SL_ASSIGN:
			case BAND_ASSIGN:
			case BXOR_ASSIGN:
			case BOR_ASSIGN:
			case QUESTION:
			case LOR:
			case LAND:
			case BOR:
			case BXOR:
			case BAND:
			case NOT_EQUAL:
			case EQUAL:
			case LT:
			case GT:
			case LE:
			case GE:
			case LITERAL_instanceof:
			case SL:
			case SR:
			case BSR:
			case PLUS:
			case MINUS:
			case DIV:
			case MOD:
			case INC:
			case DEC:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return t;
	}
	
	public final void constant() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case NUM_INT:
		{
			match(NUM_INT);
			break;
		}
		case CHAR_LITERAL:
		{
			match(CHAR_LITERAL);
			break;
		}
		case STRING_LITERAL:
		{
			match(STRING_LITERAL);
			break;
		}
		case NUM_FLOAT:
		{
			match(NUM_FLOAT);
			break;
		}
		case NUM_LONG:
		{
			match(NUM_LONG);
			break;
		}
		case NUM_DOUBLE:
		{
			match(NUM_DOUBLE);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void newArrayDeclarator() throws RecognitionException, TokenStreamException {
		
		
		{
		int _cnt190=0;
		_loop190:
		do {
			if ((LA(1)==LBRACK) && (_tokenSet_26.member(LA(2)))) {
				match(LBRACK);
				{
				switch ( LA(1)) {
				case LITERAL_void:
				case LITERAL_boolean:
				case LITERAL_byte:
				case LITERAL_char:
				case LITERAL_short:
				case LITERAL_int:
				case LITERAL_float:
				case LITERAL_long:
				case LITERAL_double:
				case IDENT:
				case LPAREN:
				case LITERAL_this:
				case LITERAL_super:
				case PLUS:
				case MINUS:
				case INC:
				case DEC:
				case BNOT:
				case LNOT:
				case LITERAL_true:
				case LITERAL_false:
				case LITERAL_null:
				case LITERAL_new:
				case NUM_INT:
				case CHAR_LITERAL:
				case STRING_LITERAL:
				case NUM_FLOAT:
				case NUM_LONG:
				case NUM_DOUBLE:
				{
					expression();
					break;
				}
				case RBRACK:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(RBRACK);
			}
			else {
				if ( _cnt190>=1 ) { break _loop190; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt190++;
		} while (true);
		}
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"BLOCK",
		"MODIFIERS",
		"OBJBLOCK",
		"SLIST",
		"CTOR_DEF",
		"METHOD_DEF",
		"VARIABLE_DEF",
		"INSTANCE_INIT",
		"STATIC_INIT",
		"TYPE",
		"CLASS_DEF",
		"INTERFACE_DEF",
		"PACKAGE_DEF",
		"ARRAY_DECLARATOR",
		"EXTENDS_CLAUSE",
		"IMPLEMENTS_CLAUSE",
		"PARAMETERS",
		"PARAMETER_DEF",
		"LABELED_STAT",
		"TYPECAST",
		"INDEX_OP",
		"POST_INC",
		"POST_DEC",
		"METHOD_CALL",
		"EXPR",
		"ARRAY_INIT",
		"IMPORT",
		"UNARY_MINUS",
		"UNARY_PLUS",
		"CASE_GROUP",
		"ELIST",
		"FOR_INIT",
		"FOR_CONDITION",
		"FOR_ITERATOR",
		"EMPTY_STAT",
		"\"final\"",
		"\"abstract\"",
		"\"strictfp\"",
		"SUPER_CTOR_CALL",
		"CTOR_CALL",
		"\"package\"",
		"SEMI",
		"\"import\"",
		"LBRACK",
		"RBRACK",
		"\"void\"",
		"\"boolean\"",
		"\"byte\"",
		"\"char\"",
		"\"short\"",
		"\"int\"",
		"\"float\"",
		"\"long\"",
		"\"double\"",
		"IDENT",
		"DOT",
		"STAR",
		"\"private\"",
		"\"public\"",
		"\"protected\"",
		"\"static\"",
		"\"transient\"",
		"\"native\"",
		"\"threadsafe\"",
		"\"synchronized\"",
		"\"volatile\"",
		"\"class\"",
		"\"extends\"",
		"\"interface\"",
		"LCURLY",
		"RCURLY",
		"COMMA",
		"\"implements\"",
		"LPAREN",
		"RPAREN",
		"\"this\"",
		"\"super\"",
		"ASSIGN",
		"\"throws\"",
		"COLON",
		"\"if\"",
		"\"else\"",
		"\"for\"",
		"\"while\"",
		"\"do\"",
		"\"break\"",
		"\"continue\"",
		"\"return\"",
		"\"switch\"",
		"\"throw\"",
		"\"assert\"",
		"\"case\"",
		"\"default\"",
		"\"try\"",
		"\"finally\"",
		"\"catch\"",
		"PLUS_ASSIGN",
		"MINUS_ASSIGN",
		"STAR_ASSIGN",
		"DIV_ASSIGN",
		"MOD_ASSIGN",
		"SR_ASSIGN",
		"BSR_ASSIGN",
		"SL_ASSIGN",
		"BAND_ASSIGN",
		"BXOR_ASSIGN",
		"BOR_ASSIGN",
		"QUESTION",
		"LOR",
		"LAND",
		"BOR",
		"BXOR",
		"BAND",
		"NOT_EQUAL",
		"EQUAL",
		"LT",
		"GT",
		"LE",
		"GE",
		"\"instanceof\"",
		"SL",
		"SR",
		"BSR",
		"PLUS",
		"MINUS",
		"DIV",
		"MOD",
		"INC",
		"DEC",
		"BNOT",
		"LNOT",
		"\"true\"",
		"\"false\"",
		"\"null\"",
		"\"new\"",
		"NUM_INT",
		"CHAR_LITERAL",
		"STRING_LITERAL",
		"NUM_FLOAT",
		"NUM_LONG",
		"NUM_DOUBLE",
		"WS",
		"SL_COMMENT",
		"ML_COMMENT",
		"ESC",
		"HEX_DIGIT",
		"EXPONENT",
		"FLOAT_SUFFIX"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { -2305803976550907904L, 383L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { -2305733607806730238L, 383L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { -2305803976550907902L, 383L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { -2305839160922996736L, 63L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { -1729941358572994560L, 383L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { -1153339868781215744L, 8575L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 864831865943490560L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 175921860444160L, 133120L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 575897802350002176L, 106496L, 130944L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	private static final long[] mk_tokenSet_9() {
		long[] data = { 1152499292141780992L, -9223372036854669312L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_9 = new BitSet(mk_tokenSet_9());
	private static final long[] mk_tokenSet_10() {
		long[] data = { -1729906174200905728L, -9223372026120395137L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_10 = new BitSet(mk_tokenSet_10());
	private static final long[] mk_tokenSet_11() {
		long[] data = { -383179802279936L, -57984440449L, 131071L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_11 = new BitSet(mk_tokenSet_11());
	private static final long[] mk_tokenSet_12() {
		long[] data = { -1729906174200905728L, -9223372026120396161L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_12 = new BitSet(mk_tokenSet_12());
	private static final long[] mk_tokenSet_13() {
		long[] data = { -1729906174200905728L, -9223372019675847041L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_13 = new BitSet(mk_tokenSet_13());
	private static final long[] mk_tokenSet_14() {
		long[] data = { -383179802279936L, -284801L, 131071L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_14 = new BitSet(mk_tokenSet_14());
	private static final long[] mk_tokenSet_15() {
		long[] data = { -1729941358572994560L, 63L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_15 = new BitSet(mk_tokenSet_15());
	private static final long[] mk_tokenSet_16() {
		long[] data = { -1153339868781215744L, 63L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_16 = new BitSet(mk_tokenSet_16());
	private static final long[] mk_tokenSet_17() {
		long[] data = { 575897802350002176L, -9223372036854669312L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_17 = new BitSet(mk_tokenSet_17());
	private static final long[] mk_tokenSet_18() {
		long[] data = { 2305455981120716800L, -68719239168L, 131071L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_18 = new BitSet(mk_tokenSet_18());
	private static final long[] mk_tokenSet_19() {
		long[] data = { -2305839160922996736L, 127L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_19 = new BitSet(mk_tokenSet_19());
	private static final long[] mk_tokenSet_20() {
		long[] data = { -2017608784771284992L, 127L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_20 = new BitSet(mk_tokenSet_20());
	private static final long[] mk_tokenSet_21() {
		long[] data = { 575897802350002176L, -9223372036854668800L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_21 = new BitSet(mk_tokenSet_21());
	private static final long[] mk_tokenSet_22() {
		long[] data = { 2305455981120716800L, -68719237120L, 131071L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_22 = new BitSet(mk_tokenSet_22());
	private static final long[] mk_tokenSet_23() {
		long[] data = { 575897802350002176L, -9223372036854145024L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_23 = new BitSet(mk_tokenSet_23());
	private static final long[] mk_tokenSet_24() {
		long[] data = { 1152921504606846976L, 0L, 6L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_24 = new BitSet(mk_tokenSet_24());
	private static final long[] mk_tokenSet_25() {
		long[] data = { 2305737456097427456L, -68718695424L, 131071L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_25 = new BitSet(mk_tokenSet_25());
	private static final long[] mk_tokenSet_26() {
		long[] data = { 576179277326712832L, -9223372036854669312L, 131065L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_26 = new BitSet(mk_tokenSet_26());
	
	}
