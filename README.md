# ChimpiNFC 🐒📲

Aplicativo Android independente para **ler e gravar etiquetas NFC NDEF regraváveis** e **ler os links públicos NFC de AirTags da Apple**

> É um repositório separado do Chimpi3D. Um AirTag da Apple não é uma TAG NFC regravável, portanto não é possível reprogramá-lo ou cloná-lo com este aplicativo

## Recursos

- **Leitura NFC:** textos e links NDEF aproximando uma etiqueta
- **Gravação NFC:** textos e URLs HTTP/HTTPS em etiquetas próprias que suportem NDEF e escrita
- **Modo seguro:** a gravação requer confirmação e vale só para a próxima etiqueta detectada
- **AirTag:** reconhecimento de links oficiais públicos `https://found.apple.com/`, sem abrir automaticamente
- **Sem localização, clonagem ou alteração de AirTags**
- **GitHub Actions:** testa o projeto e gera um APK de depuração em um pré-Release

## Compatibilidade

- Android 8 ou superior (API 26+)
- Para leitura ou gravação, o aparelho precisa ter **NFC físico** e NFC ativado
- Gravação depende de etiqueta **NDEF regravável** e de capacidade suficiente
- TAGs bloqueadas, AirTags originais e tecnologias não-NDEF não são graváveis pelo app

## Publicar APK pelo GitHub

Acesse **Actions → Android • Compilar APK e publicar Release → Run workflow**, ou envie uma alteração de código para a branch `main`

Quando o trabalho passar, entre em **Releases** e baixe `ChimpiNFC-Android-debug.apk`

O APK usa **assinatura de depuração para testes**, não uma assinatura de produção da Play Store

## Como usar

1. Ative o NFC nas configurações de um celular compatível
2. Para ler: aproxime uma etiqueta NFC ou um AirTag e veja **Última leitura**
3. Para gravar: selecione **Texto** ou **Link**, digite o conteúdo, toque em **Preparar gravação** e confirme
4. Aproxime uma etiqueta NFC regravável de sua propriedade ou autorizada
5. Para cancelar a gravação pendente, toque em **Cancelar gravação / Apenas ler**

**Atenção:** gravar substitui o conteúdo anterior da etiqueta. O app não envia informações lidas a servidores e não abre URLs automaticamente

## Compilação local

JDK 17, Android SDK 35, Gradle 8.9:

```bash
gradle --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

## Referências

- [Apple: como obter informações de um AirTag encontrado](https://support.apple.com/pt-br/119874)
- [Android NDEF](https://developer.android.com/reference/android/nfc/tech/Ndef)

## Licença

MIT • Chimpi-hackintosh
